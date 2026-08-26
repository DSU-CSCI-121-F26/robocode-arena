# Robocode — CSCI-121

You write a Java class. It becomes a tank. It fights other people's tanks.

Every chapter you learn in class has an upgrade you can make to your bot, and on the
last day of class there is a tournament.

**None of this is required.** It is here because the fastest way to understand a loop
is to watch a loop drive a tank into a wall.

---

## Setup (once)

You already have IntelliJ, Git, and Maven from Recitation 0.

**1. Install Robocode.** Download the installer from
[robocode.sourceforge.io](https://robocode.sourceforge.io/), then run it:

```
java -jar robocode-1.11.1-setup.jar
```

Accept the default location (`~/robocode`). Remember where it went.

**2. Fork this repo** and clone your fork. Your fork is yours — experiment freely.

**3. Build it.** From the folder this README is in:

```
./mvnw test
```

Windows: use `mvnw.cmd` instead of `./mvnw` everywhere in this README. Seven tests
should pass. That means Java, Maven, and this project agree with each other.

**4. Make your own bot.**

```
./tourney init <your-github-username> ThunderTank
```

That creates `src/main/java/bots/<your-username>/ThunderTank.java`. Edit that file.
Leave everyone else's folder alone.

> Your folder is named after you because every bot in a battle needs a unique name, and
> four people will pick `ThunderTank`. GitHub usernames are already unique.

---

## The loop

```
1. Edit your bot in IntelliJ
2. ./tourney --samples     compile, fight, see the score
3. Groan. Go back to step 1.
```

That is the whole thing. `./tourney` compiles everything for you.

**Try it right now.** Open your bot and find:

```java
private static final double MOVE_DISTANCE = 100;
```

Change `100` to `20`, run `./tourney --samples`, and see how the score changes. Then
try `400`. You just used a variable to change the behavior of a program, which is the
entire point of Chapter 2.

---

## Watching a battle

`./tourney` runs with the display off — a tournament that opens 40 windows is not a
tournament. To actually *watch*, use the Robocode GUI. Two extra steps:

**Run Robocode from IntelliJ.** This repo ships a run configuration, so **Robocode**
should appear in the run dropdown, top right. Press the green arrow.

If you installed Robocode somewhere other than `~/robocode`, **two** settings need the
new path (Run → Edit Configurations → Robocode):

- **Working directory** → your Robocode install
- **Modify options → Modify classpath** → the `robocode.jar` entry

**Point Robocode at your bot.** With Robocode open:

> Options → Preferences → Development Options → Add

Add exactly this path — not `src`, not the repo root:

```
<this repo>/target/classes
```

Run `./mvnw compile` first so the folder exists. Then press **Cmd+R** in the New Battle
dialog to refresh the list. Your bot shows up under `bots.<your-username>`.

> Online guides say to add `out/production/<project>`. That is for non-Maven projects.
> **Yours is `target/classes`.**

Now: Battle → New → pick your bot and an enemy → Start Battle.

**Good first opponents:** `sample.SittingDuck` (cannot fight back), then
`sample.Corners`, then `sample.Crazy`. `sample.Walls` will beat you for a while.

> **The gotcha that will cost you twenty minutes.** Robocode reads `target/classes`, not
> your source code. Edit, then **`./mvnw compile`**, then Cmd+R. And if you run
> `./mvnw clean`, that folder disappears and your bot vanishes from the list until you
> compile again.

---

## Fighting your classmates

```
./tourney                     every bot in your copy of the repo
./tourney alice bob           just you against alice and bob
./tourney --list              who is currently in the repo
```

The scoreboard prints when it is done:

```
  RANK  BOT                                  SCORE      ROUNDS WON
  ------------------------------------------------------------------
     1  octocat/ThunderTank                    498              4
     2  hornet/HornetBot                       219              1
```

A bot that does not compile is reported and dropped. Everyone else still fights — you
are never blocked by someone else's broken code.

### Getting their bots

Once, ever (the URL is on this repo's green **Code** button — the repo you forked
*from*, not your fork):

```
git remote add upstream https://github.com/DSU-CSCI-121-F26/robocode-arena.git
```

Then any time you want everyone's latest:

```
git pull upstream main
```

### Publishing yours

Nobody can fight your bot until you publish it. Publishing is a pull request:

```
git switch -c my-bot
git add src/main/java/bots/<your-username>/
git commit -m "Add ThunderTank"
git push -u origin my-bot
```

Then open a pull request against this repo. **Only ever touch files inside your own
folder** — CI rejects the PR if you stray outside it. Push improvements any time; there
is no deadline and no limit.

---

## Options

| Flag | What it does |
|---|---|
| `--rounds N` | Rounds per battle. Default 10. |
| `--all` | Every bot in the repo. Same as no arguments. |
| `--samples` | Add `sample.Corners`, `Crazy`, and `Walls` as opponents. Automatic if you are the only bot. |
| `--pairwise` | Round robin — every bot fights every other one-on-one. Slower and much fairer than one big melee. |
| `--list` | Print the bots it can see, then stop. |
| `--robocode <path>` | Where Robocode is installed, if not `~/robocode`. |
| `init <user> <BotName>` | Create your own bot folder. |

---

## One upgrade per week

Do these as the matching week comes up in class. Each one is small.

| Week | Upgrade to try |
|---|---|
| **2** — Variables and types | Change `MOVE_DISTANCE`, `DODGE_DISTANCE`, and `DODGE_TURN`. Find values that beat `sample.Corners`. |
| **3** — Your first class | Add a small class that remembers the last place you scanned an enemy. Give it fields and a constructor. |
| **4** — Objects interacting | Give your bot a colour scheme with `setColors()`. Then have your bot *ask* that memory class where to aim. |
| **5** — Methods | The `run()` loop is getting crowded. Pull the driving out into a `patrol()` method and call it. |
| **6** — Branching, loops, enums | Only fire if the enemy is closer than 300 — save your energy. Then add `private enum Mode { PATROL, ENGAGE, EVADE }` and a field to hold the current one. Nothing has to use it yet. |
| **7** — State machines | Now use it. Wrap `run()` in `while (true) { switch (mode) { ... } }`: patrol until you scan someone, engage until you get hit, evade, then back to patrol. **Most useful thing in this list — see below.** |
| **9** — Arrays | Keep the last 10 places you scanned an enemy. Are they circling you? |
| **10** — Inheritance | Change `extends Robot` to `extends AdvancedRobot` for non-blocking movement and much more control. Then split your bot into a base class and two subclasses with different personalities. |
| **11** — Polymorphism and interfaces | Make both subclasses satisfy one interface — say `Targeting` with a single `aimAt(ScannedRobotEvent)` method — and swap strategies without touching the bot. |
| **13** — Exceptions | What happens when your targeting math divides by zero because the enemy is exactly on top of you? Find out. Handle it. |

---

## This is your term project in disguise

Your tank and the maze rover you build with your group are the same shape of program:

```java
// Your bot, here
public class ThunderTank extends Robot {
    public void run() {
        while (true) {
            // decide, then act
        }
    }
}

// The term project rover
public class MazeRobot extends RobotController {
    private enum RobotState { CRUISE, IDENTIFY_OBJECT, AVOID_OBJECT, ... }

    public void run() {
        while (currentState != RobotState.STOP) {
            switch (currentState) {
                // decide, then act
            }
        }
    }
}
```

Both extend a framework class you did not write. Both do everything in `run()`. Both
loop until they are done. The rover just has an enum and a switch inside — which is the
week 7 upgrade above.

**That is why this repo is worth your time.** The rover is slow to experiment on: a
battery, a pairing, floor conditions, and three teammates' schedules per attempt. Your
tank recompiles in one second. Every state-machine mistake you are going to make — the
state that never transitions, the transition with no matching `case`, the state you set
but never act on — is cheaper to make here first.

> One real difference: Robocode is *event-driven* (you write `onScannedRobot`, the
> framework calls it). The rover *polls* (your loop asks the sensors, then decides).
> Two ways to structure a control program, and you will have written both.

---

## About the tests

Look at `BotMath.java` and `BotMathTest.java`.

A `Robot` only runs inside a battle, which makes it painful to test. So the math lives
in `BotMath` instead — plain static methods, no robot involved — and JUnit checks them
in milliseconds.

| Hard to test | Easy to test |
|---|---|
| `ahead(100)` — needs a battle | `firePowerFor(distance)` — needs nothing |
| `mbot.followLine()` — needs a rover, a floor, and a battery | *"given this colour, which state comes next?"* — needs nothing |

**Split the decision from the action.** Actions have to be tried on the real thing.
Decisions do not — and decisions are where your bugs live. Pull them into plain methods
and you can test the brain of your robot at midnight with no hardware in the room. You
will do exactly this to your term project robot later in the semester.

So when you add real logic — targeting, dodging, deciding when to fire — put the
calculation in `BotMath` and write a test for it.

Run tests any time with `./mvnw test`. They also run on every push, so you get a green
check or a red X on GitHub.

---

## Write down the design

This repo has a `design.md`. Fill it in before your bot grows past two classes — a short
Mermaid class diagram of what is in here and what it does. GitHub renders it, so anyone
looking at your repo can see the shape of your bot without reading code.

The course's UML materials have the tutorial and a cheat sheet.

---

## The tournament

Last day of class. Whatever bot you have published upstream by then is the bot that
fights — so publish early, and publish again whenever you improve it.

The bracket is `./tourney --all --pairwise`, run on the instructor's machine. Nothing
about it is special: it is the same command you have been running all semester.

Entering earns extra credit. Winning earns bragging rights, which last longer.

---

## Using AI on this

This repo is **Tier 1 — AI-assisted, with disclosure.** Use a model to explain the
Robocode API, review your bot, or help you debug. Add an `AI-USE.md` saying what you
asked for and what you changed.

The usual standard: if you cannot explain why your bot does what it does, you do not
have a bot — you have someone else's bot.

One warning that will save you time: models confidently invent Robocode methods that do
not exist. When one tells you about `robocode.Robot#superAimBot()`, check the
[real API docs](https://robocode.sourceforge.io/docs/robocode/) before you go hunting
for why it will not compile.

---

## When something breaks

| Symptom | Fix |
|---|---|
| Changes do not take effect | You edited but did not recompile. `./mvnw compile` |
| `./tourney` does not list your bot | It must be `public`, `extends Robot` (or `AdvancedRobot`), and live under `src/main/java/bots/<your-username>/`. Run `./tourney --list` to see what it finds |
| **The GUI** does not list your bot | In order: (1) `./mvnw compile` — `clean` deletes `target/classes` and your bot with it, (2) press **Cmd+R** in the New Battle dialog, (3) check Development Options points at exactly `target/classes`, (4) look under `bots.<your-username>` |
| `./tourney` says Robocode is not installed | It looks in `~/robocode`. If yours is elsewhere: `./tourney --robocode /path/to/robocode`, or set `ROBOCODE_HOME` |
| A classmate's bot is missing | Either they have not published it, or you have not run `git pull upstream main`. If it failed to compile, `./tourney` says so |
| "Robocode" missing from the run dropdown | IntelliJ did not pick up `.run/`. File → Reload All from Disk, or re-open the project |
| `ClassNotFoundException: robocode.Robocode` | Maven has not reimported. Click **Reload All Maven Projects** in the Maven panel |
| `cannot find symbol: class Robot` | Run `./mvnw compile` once so Maven downloads the Robocode API |
| Robocode starts then immediately dies | The run configuration's working directory is not your Robocode install |
| Console says `Loading plugins from` the wrong folder | Same — wrong working directory |
| Double-clicking `robocode.command` says `./robocode.sh: No such file or directory` | Robocode's launcher does not `cd` first. Add `cd "$(dirname "$0")"` above the last line |
| Bot appears but instantly dies | Probably an exception in `run()`. Robocode prints the stack trace to the console — read it |
