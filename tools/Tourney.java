import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * Runs Robocode battles between the bots in this repository.
 *
 * You do not need to read this to use it — see the README. But you can read it,
 * and by week 9 most of it will make sense. It is ordinary Java: no framework,
 * no dependencies, nothing you will not meet in this course.
 *
 * <p>How it works, in four steps:
 *
 * <pre>
 *   1. find every bot     src/main/java/bots/&lt;owner&gt;/*.java
 *   2. compile each one   separately, so one broken bot cannot break the rest
 *   3. package each one   into a .jar, because that is the only form headless
 *                         Robocode will actually load
 *   4. run the battle     and print who won
 * </pre>
 *
 * <p>Step 3 is the non-obvious one. Robocode's "development path" — the setting you
 * add in Preferences so your bot shows up in the GUI — is ignored when Robocode runs
 * with {@code -nodisplay}. Loose {@code .class} files are invisible to a headless
 * battle no matter where you point it. A packaged jar in the robots directory is the
 * one thing that works in both modes, so that is what this builds.
 */
public class Tourney {

    /** How many rounds each battle runs, unless --rounds says otherwise. */
    private static final int DEFAULT_ROUNDS = 10;

    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;

    /** Stand-in opponents, used when you are the only bot in the room. */
    private static final List<String> SAMPLE_OPPONENTS =
            List.of("sample.Corners", "sample.Crazy", "sample.Walls");

    /** Where bots live. One folder per person, named for their GitHub username. */
    private static final Path BOTS_DIR = Path.of("src", "main", "java", "bots");

    /** Shared, tested helper code that every bot may use. Goes into every jar. */
    private static final Path ARENA_DIR = Path.of("src", "main", "java", "arena");

    /** Scratch space. Everything here is disposable and gitignored. */
    private static final Path WORK = Path.of(".tourney");

    /** Matches the package line so we can work out a bot's full name. */
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    /** Matches a class that extends any of Robocode's robot base classes. */
    private static final Pattern ROBOT_CLASS = Pattern.compile(
            "(?m)^\\s*public\\s+class\\s+(\\w+)\\s+extends\\s+"
                    + "(?:robocode\\.)?(AdvancedRobot|TeamRobot|JuniorRobot|Robot)\\b");

    // ----------------------------------------------------------------------
    // A bot we found on disk.
    // ----------------------------------------------------------------------

    /**
     * One competitor.
     *
     * @param owner       the folder name under bots/, which is a GitHub username
     * @param fullName    the fully-qualified class name Robocode battles by
     * @param simpleName  just the class name, for display
     * @param sourceDir   the folder holding this bot's source
     */
    record Bot(String owner, String fullName, String simpleName, Path sourceDir) {
        @Override
        public String toString() {
            return fullName;
        }
    }

    /** One row of a Robocode results file. */
    record Score(String botName, int points, int firsts) {}

    // ----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        List<String> rest = new ArrayList<>(List.of(args));

        if (!rest.isEmpty() && rest.get(0).equals("init")) {
            rest.remove(0);
            initBot(rest);
            return;
        }

        boolean all = rest.remove("--all");
        boolean pairwise = rest.remove("--pairwise");
        boolean listOnly = rest.remove("--list");
        boolean forceSamples = rest.remove("--samples");
        int rounds = intOption(rest, "--rounds", DEFAULT_ROUNDS);
        Path robocodeHome = robocodeHome(stringOption(rest, "--robocode", null));

        List<Bot> found = discover();
        if (found.isEmpty()) {
            fail("No bots found under " + BOTS_DIR + ".\n"
                    + "Run:  ./tourney init <your-github-username> <BotName>");
        }

        if (listOnly) {
            System.out.println("Bots in this repository:\n");
            for (Bot b : found) {
                System.out.printf("  %-20s %s%n", b.owner(), b.fullName());
            }
            System.out.println("\n" + found.size() + " bot(s). Run './tourney --all' to fight them all.");
            return;
        }

        List<Bot> selected = select(found, rest, all);

        System.out.println("Compiling " + selected.size() + " bot(s)...");
        Path robots = WORK.resolve("robots");
        deleteTree(robots);
        Files.createDirectories(robots);

        List<Bot> ready = new ArrayList<>();
        for (Bot bot : selected) {
            if (build(bot, robots, robocodeHome)) {
                ready.add(bot);
            }
        }

        if (ready.isEmpty()) {
            fail("Nothing compiled. Fix the errors above and try again.");
        }

        // Robocode reads exactly one robots directory, so the sample bots have to be
        // copied in alongside ours rather than referenced where they live.
        copyTree(robocodeHome.resolve("robots").resolve("sample"), robots.resolve("sample"));

        List<String> entrants = new ArrayList<>(ready.stream().map(Bot::fullName).toList());
        if (forceSamples || entrants.size() < 2) {
            if (entrants.size() < 2) {
                System.out.println("Only one bot — bringing in the sample bots so you have someone to fight.");
            }
            entrants.addAll(SAMPLE_OPPONENTS);
        }

        if (pairwise) {
            runPairwise(entrants, rounds, robocodeHome, robots);
        } else {
            System.out.println("\nBattle: " + String.join(", ", entrants));
            System.out.println("Rounds: " + rounds + "\n");
            List<Score> scores = runBattle(entrants, rounds, robocodeHome, robots);
            printMelee(scores);
        }
    }

    // ----------------------------------------------------------------------
    // Finding bots
    // ----------------------------------------------------------------------

    /**
     * Walks the bots folder and returns every robot class it can find.
     *
     * A file counts as a bot if it declares a public class extending one of
     * Robocode's robot types. Helper classes in the same folder are ignored here
     * but still get compiled and packaged with their bot.
     */
    private static List<Bot> discover() throws IOException {
        List<Bot> bots = new ArrayList<>();
        if (!Files.isDirectory(BOTS_DIR)) {
            return bots;
        }
        try (Stream<Path> owners = Files.list(BOTS_DIR)) {
            for (Path ownerDir : owners.filter(Files::isDirectory).sorted().toList()) {
                try (Stream<Path> sources = Files.list(ownerDir)) {
                    for (Path src : sources.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                        String text = Files.readString(src);
                        Matcher robot = ROBOT_CLASS.matcher(text);
                        Matcher pkg = PACKAGE.matcher(text);
                        if (robot.find() && pkg.find()) {
                            bots.add(new Bot(
                                    ownerDir.getFileName().toString(),
                                    pkg.group(1) + "." + robot.group(1),
                                    robot.group(1),
                                    ownerDir));
                        }
                    }
                }
            }
        }
        return bots;
    }

    /** Narrows the full bot list down to the ones named on the command line. */
    private static List<Bot> select(List<Bot> found, List<String> names, boolean all) {
        if (all || names.isEmpty()) {
            return found;
        }
        List<Bot> chosen = new ArrayList<>();
        for (String name : names) {
            List<Bot> matches = found.stream()
                    .filter(b -> b.owner().equalsIgnoreCase(name)
                            || b.simpleName().equalsIgnoreCase(name)
                            || b.fullName().equalsIgnoreCase(name))
                    .toList();
            if (matches.isEmpty()) {
                System.out.println("  ! no bot called '" + name + "' — skipping. "
                        + "Try './tourney --list', or 'git pull upstream main' to get their latest.");
            }
            chosen.addAll(matches);
        }
        if (chosen.isEmpty()) {
            fail("None of those bots exist here. './tourney --list' shows what does.");
        }
        return chosen;
    }

    // ----------------------------------------------------------------------
    // Building one bot into a jar
    // ----------------------------------------------------------------------

    /**
     * Compiles and packages a single bot, returning false if it does not build.
     *
     * Each bot compiles on its own into its own output folder. That isolation is
     * the point: when a classmate pushes a bot that does not compile, their bot
     * drops out of the battle and yours still runs.
     */
    private static boolean build(Bot bot, Path robotsDir, Path robocodeHome) throws IOException {
        Path classes = WORK.resolve("classes").resolve(bot.owner());
        deleteTree(classes);
        Files.createDirectories(classes);

        List<String> sources = new ArrayList<>();
        collectJavaFiles(bot.sourceDir(), sources);
        collectJavaFiles(ARENA_DIR, sources);

        List<String> javacArgs = new ArrayList<>(List.of(
                "-cp", robocodeHome.resolve("libs").resolve("robocode.jar").toString(),
                "-d", classes.toString(),
                "-nowarn"));
        javacArgs.addAll(sources);

        StringWriter errors = new StringWriter();
        ToolProvider javac = ToolProvider.findFirst("javac")
                .orElseThrow(() -> new IllegalStateException(
                        "No javac available. Are you running a JDK rather than a JRE?"));
        int rc = javac.run(new PrintWriter(errors), new PrintWriter(errors),
                javacArgs.toArray(new String[0]));

        if (rc != 0) {
            System.out.println("  x " + bot.owner() + "/" + bot.simpleName() + " did not compile:");
            errors.toString().lines().limit(12).forEach(l -> System.out.println("      " + l));
            return false;
        }

        writeRobotProperties(bot, classes);

        Path jar = robotsDir.resolve(bot.owner() + "-" + bot.simpleName() + ".jar");
        ToolProvider jarTool = ToolProvider.findFirst("jar").orElseThrow();
        int jarRc = jarTool.run(System.out, System.err,
                "--create", "--file", jar.toString(), "-C", classes.toString(), ".");
        if (jarRc != 0) {
            System.out.println("  x " + bot.owner() + " could not be packaged");
            return false;
        }

        System.out.println("  ok " + bot.owner() + "/" + bot.simpleName());
        return true;
    }

    /**
     * Writes the descriptor Robocode uses to recognise a class as a robot.
     *
     * Without this file the jar is just a jar and the bot silently never appears
     * in the battle. It goes next to the class, inside the package folder.
     */
    private static void writeRobotProperties(Bot bot, Path classes) throws IOException {
        String packagePath = bot.fullName().substring(0, bot.fullName().lastIndexOf('.'))
                .replace('.', '/');
        Path target = classes.resolve(packagePath).resolve(bot.simpleName() + ".properties");
        Files.createDirectories(target.getParent());
        String body = """
                #Robot Properties
                robot.description=CSCI-121 bot by %s
                robot.webpage=
                robocode.version=1.1.2
                robot.java.source.included=false
                robot.author.name=%s
                robot.classname=%s
                robot.name=%s
                """.formatted(bot.owner(), bot.owner(), bot.fullName(), bot.simpleName());
        Files.writeString(target, body);
    }

    private static void collectJavaFiles(Path dir, List<String> into) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toString)
                    .sorted()
                    .forEach(into::add);
        }
    }

    // ----------------------------------------------------------------------
    // Running battles
    // ----------------------------------------------------------------------

    /**
     * Runs one battle and returns the scoreboard.
     *
     * Robocode has to be started from its own install folder — that is how it finds
     * its plugins — so the working directory is set there and every path we hand it
     * is absolute.
     */
    private static List<Score> runBattle(List<String> entrants, int rounds,
                                         Path robocodeHome, Path robotsDir) throws Exception {
        Files.createDirectories(WORK);
        Path battle = WORK.resolve("tourney.battle").toAbsolutePath();
        Path results = WORK.resolve("results.txt").toAbsolutePath();
        Files.deleteIfExists(results);

        Files.writeString(battle, """
                #Battle Properties
                robocode.battleField.width=%d
                robocode.battleField.height=%d
                robocode.battle.numRounds=%d
                robocode.battle.gunCoolingRate=0.1
                robocode.battle.rules.inactivityTime=450
                robocode.battle.selectedRobots=%s
                """.formatted(FIELD_WIDTH, FIELD_HEIGHT, rounds, String.join(",", entrants)));

        List<String> command = List.of(
                javaExecutable(),
                "-Xmx512M",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-Dsun.io.useCanonCaches=false",
                "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "-DROBOTPATH=" + robotsDir.toAbsolutePath(),
                "-cp", "libs/*",
                "robocode.Robocode",
                "-battle", battle.toString(),
                "-results", results.toString(),
                "-nodisplay",
                "-nosound");

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(robocodeHome.toFile())
                .redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();

        output.lines()
                .filter(l -> l.contains("Can't find"))
                .distinct()
                .forEach(l -> System.out.println("  ! " + l + " (did it compile?)"));

        if (!Files.exists(results)) {
            System.out.println(output);
            fail("Robocode produced no results. The output above should say why.");
        }
        return parseResults(Files.readString(results));
    }

    /**
     * Reads Robocode's results file.
     *
     * The format is tab-separated with a rank prefix, like:
     * {@code 1st: bots.hornet.HornetBot<TAB>360 (100%)<TAB>100<TAB>...}
     */
    private static List<Score> parseResults(String text) {
        Pattern row = Pattern.compile("^\\s*\\d+(?:st|nd|rd|th):\\s*(.+?)\\t\\s*(\\d+)");
        List<Score> scores = new ArrayList<>();
        for (String line : text.split("\n")) {
            Matcher m = row.matcher(line);
            if (m.find()) {
                String[] cells = line.split("\t");
                int firsts = 0;
                // The "1sts" column is 8 cells past the score on a standard results row.
                if (cells.length >= 9) {
                    try {
                        firsts = Integer.parseInt(cells[8].trim());
                    } catch (NumberFormatException ignored) {
                        // Column layout varies with the Robocode version; points still work.
                    }
                }
                // Robocode appends the robot version to the name, eg "MyBot 1.0".
                String name = m.group(1).trim().split("\\s+")[0];
                scores.add(new Score(name, Integer.parseInt(m.group(2)), firsts));
            }
        }
        return scores;
    }

    /** Every bot fights every other bot one-on-one. Slower, but a fairer ranking. */
    private static void runPairwise(List<String> entrants, int rounds,
                                    Path robocodeHome, Path robotsDir) throws Exception {
        Map<String, Integer> wins = new LinkedHashMap<>();
        Map<String, Integer> points = new LinkedHashMap<>();
        entrants.forEach(e -> {
            wins.put(e, 0);
            points.put(e, 0);
        });

        int matches = entrants.size() * (entrants.size() - 1) / 2;
        System.out.println("\nRound robin: " + matches + " matches, " + rounds + " rounds each.\n");

        int n = 0;
        for (int i = 0; i < entrants.size(); i++) {
            for (int j = i + 1; j < entrants.size(); j++) {
                String a = entrants.get(i);
                String b = entrants.get(j);
                n++;
                System.out.printf("  [%d/%d] %s vs %s%n", n, matches, shortName(a), shortName(b));

                List<Score> scores = runBattle(List.of(a, b), rounds, robocodeHome, robotsDir);
                scores.forEach(s -> points.merge(s.botName(), s.points(), Integer::sum));
                scores.stream()
                        .max(Comparator.comparingInt(Score::points))
                        .ifPresent(best -> wins.merge(best.botName(), 1, Integer::sum));
            }
        }

        System.out.println("\n  RANK  BOT                             MATCHES WON   TOTAL POINTS");
        System.out.println("  " + "-".repeat(66));
        List<String> ranked = entrants.stream()
                .sorted(Comparator.<String>comparingInt(e -> wins.getOrDefault(e, 0))
                        .thenComparingInt(e -> points.getOrDefault(e, 0)).reversed())
                .toList();
        for (int i = 0; i < ranked.size(); i++) {
            String e = ranked.get(i);
            System.out.printf("  %4d  %-30s %11d %14d%n",
                    i + 1, shortName(e), wins.getOrDefault(e, 0), points.getOrDefault(e, 0));
        }
        System.out.println();
    }

    private static void printMelee(List<Score> scores) {
        System.out.println("  RANK  BOT                                  SCORE      ROUNDS WON");
        System.out.println("  " + "-".repeat(66));
        for (int i = 0; i < scores.size(); i++) {
            Score s = scores.get(i);
            System.out.printf("  %4d  %-30s %11d %14d%n",
                    i + 1, shortName(s.botName()), s.points(), s.firsts());
        }
        System.out.println();
    }

    /** Turns {@code bots.alice.ThunderTank} into {@code alice/ThunderTank} for display. */
    private static String shortName(String fullName) {
        String[] parts = fullName.split("\\.");
        if (parts.length >= 3 && parts[0].equals("bots")) {
            return parts[1] + "/" + parts[parts.length - 1];
        }
        return fullName;
    }

    // ----------------------------------------------------------------------
    // init — scaffolding a new bot
    // ----------------------------------------------------------------------

    /**
     * Creates a new bot folder by copying the reference bot and renaming it.
     *
     * The folder is named for the student's GitHub username, which is what keeps
     * 25 bots from colliding: usernames are unique, so packages are unique, so
     * two people can both call their tank Thunder without Robocode caring.
     */
    private static void initBot(List<String> args) throws IOException {
        if (args.size() != 2) {
            fail("Usage: ./tourney init <your-github-username> <BotName>\n"
                    + "   eg: ./tourney init octocat ThunderTank");
        }
        String username = args.get(0).toLowerCase();
        String botName = args.get(1);

        if (!username.matches("[a-z0-9][a-z0-9-]*")) {
            fail("'" + username + "' does not look like a GitHub username.");
        }
        if (!botName.matches("[A-Z][A-Za-z0-9]*")) {
            fail("'" + botName + "' is not a valid Java class name. Start with a capital letter.");
        }

        // Java package names cannot contain hyphens, so a username like 'ada-l' becomes 'ada_l'.
        String packageSegment = username.replace('-', '_');
        Path dir = BOTS_DIR.resolve(username);
        if (Files.exists(dir)) {
            fail(dir + " already exists. Your bot is already set up.");
        }

        Path template = BOTS_DIR.resolve("hornet").resolve("HornetBot.java");
        if (!Files.exists(template)) {
            fail("Cannot find the reference bot at " + template);
        }

        Files.createDirectories(dir);
        String source = Files.readString(template)
                .replace("package bots.hornet;", "package bots." + packageSegment + ";")
                .replace("HornetBot", botName)
                .replace("The reference bot that ships with the arena. It is a real opponent — beat it.",
                        "%s, by %s.".formatted(botName, username));
        source = stripDoNotEditNote(source);

        Path target = dir.resolve(botName + ".java");
        Files.writeString(target, source);

        System.out.println("Created " + target);
        System.out.println("""

                Next:
                  1. Open it and change MOVE_DISTANCE.
                  2. ./tourney                  see how it does
                  3. Commit, push to your fork, and open a pull request when you
                     want everyone else to be able to fight it.
                """);
    }


    /**
     * Removes the "do not edit this file" note from the copy.
     *
     * That warning belongs to the reference bot. On your own copy it would be exactly
     * wrong — editing it is the whole point — so it is stripped out here rather than
     * left to confuse you.
     */
    private static String stripDoNotEditNote(String source) {
        List<String> lines = new ArrayList<>(source.lines().toList());
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("You do not edit this file")) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return source;
        }
        int end = start;
        while (end < lines.size() && !lines.get(end).contains("edit that.")) {
            end++;
        }
        // Take the blank javadoc line above the note with it, so no gap is left behind.
        if (start > 0 && lines.get(start - 1).strip().equals("*")) {
            start--;
        }
        lines.subList(start, Math.min(end + 1, lines.size())).clear();
        return String.join("\n", lines) + "\n";
    }

    // ----------------------------------------------------------------------
    // Small helpers
    // ----------------------------------------------------------------------

    /**
     * Works out where Robocode is installed and checks that it really is there.
     *
     * Order of preference: the --robocode flag, the ROBOCODE_HOME environment
     * variable, then ~/robocode, which is where the installer puts it by default.
     */
    private static Path robocodeHome(String override) {
        String candidate = override;
        if (candidate == null) {
            candidate = System.getenv("ROBOCODE_HOME");
        }
        if (candidate == null) {
            candidate = System.getProperty("user.home") + "/robocode";
        }
        Path home = Path.of(candidate);
        if (!Files.exists(home.resolve("libs").resolve("robocode.jar"))) {
            fail("""
                 Robocode is not installed at %s

                 Install it from https://robocode.sourceforge.io/ (the README has the steps),
                 or point this at your install:

                     ./tourney --robocode /path/to/robocode
                     export ROBOCODE_HOME=/path/to/robocode
                 """.formatted(home));
        }
        return home;
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static int intOption(List<String> args, String flag, int fallback) {
        String value = stringOption(args, flag, null);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static String stringOption(List<String> args, String flag, String fallback) {
        int i = args.indexOf(flag);
        if (i < 0) {
            return fallback;
        }
        if (i + 1 >= args.size()) {
            fail(flag + " needs a value after it.");
        }
        String value = args.get(i + 1);
        args.remove(i + 1);
        args.remove(i);
        return value;
    }

    private static void copyTree(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path src : walk.toList()) {
                Path dest = to.resolve(from.relativize(src).toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void fail(String message) {
        System.err.println("\n" + message + "\n");
        System.exit(1);
    }
}
