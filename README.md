# Robocode — CSCI-121

You write a Java class. It becomes a tank. It fights other people's tanks.

This repo runs alongside the whole course. Every chapter you learn has an upgrade
you can make to your bot, and at the end of the semester there is a tournament.

**Fork it.** Your fork is yours: you work in your own folder, and when you want other
people to be able to fight your bot, you open a pull request back here. `./tourney`
runs battles between every bot in the repo, so fighting your classmates is one command.

**None of this is required.** It is here because the fastest way to understand a
loop is to watch a loop drive a tank into a wall.

---

## One-time setup

You already have IntelliJ, Git, and Maven from Recitation 0. You need one more thing.

**1. Install Robocode.**

Download the installer from [robocode.sourceforge.io](https://robocode.sourceforge.io/),
then run it:

```
java -jar robocode-1.11.1-setup.jar
```

Accept the default install location. Write down where it put things — you will
not need it often, but you will need it once.

**2. Build this project.**

From the folder this README is in:

```
./mvnw test
```

On Windows, use `mvnw.cmd test` instead. You should see seven tests pass. That
means Java, Maven, and this project all agree with each other.

**3. Run Robocode from IntelliJ.**

This repo ships a run configuration. Open the project in IntelliJ and you should see
**Robocode** in the run dropdown, top right. Press the green arrow.

The configuration assumes you installed Robocode to `~/robocode`. If you put it
somewhere else, **two** settings need the new path:

> **Run → Edit Configurations → Robocode**
> - **Working directory** → your Robocode install
> - **Modify options → Modify classpath** → the `robocode.jar` entry

Both matter, for different reasons. The working directory is how Robocode finds its own
plugins — watch the console on startup and you will see `Loading plugins from <dir>/libs`.
The classpath entry is `robocode.jar`, which contains the main class and is the one piece
Robocode does not publish to Maven Central. That is also why it is **not** in `pom.xml`:
the build and CI only need `robocode.api`, which is on Central.

**4. Tell Robocode where your bot is.** *(only needed for the GUI — `./tourney` does
not need this and works straight away)*

With Robocode open:

> **Options → Preferences → Development Options → Add**

Add this exact path:

```
<this repo>/target/classes
```

Not `src`, not the repo root — `target/classes`, the folder Maven compiles into. Run
`./mvnw compile` first so the folder exists.

> Guides written for non-Maven projects tell you to add `out/production/<project>`.
> That is IntelliJ's own output folder. **This is a Maven project, so yours is
> `target/classes`.**

Then in the New Battle dialog press **Cmd+R** to refresh the robot list — no need to
restart Robocode.

Bots appear in the Packages list under `bots.<username>` — yours under your own
GitHub username, the reference bot under `bots.hornet`.

> **The gotcha that will cost you twenty minutes.** Robocode reads `target/classes`, not
> your source. If you run `./mvnw clean`, that folder disappears and **your bot vanishes
> from the battle list.** Run `./mvnw compile` and press Cmd+R to bring it back. Same if
> you have never compiled at all.

---

## The loop you will repeat all semester

```
1. Edit your bot in IntelliJ
2. ./tourney --samples     compile, fight, see the score
3. Groan. Go back to step 1.
```

When you want to *watch* rather than just score, use the GUI instead:

```
1. Edit your bot in IntelliJ
2. ./mvnw compile          (or IntelliJ's Build > Build Project)
3. In Robocode:  Battle → New → pick your bot + an enemy → Start Battle
4. Watch. Groan. Go back to step 1.
```

Robocode picks up your recompiled bot automatically — you do not need to restart it.
**But you do have to compile.** Editing the file is not enough; Robocode reads
`target/classes`, not your source.

---

## Your first change, right now

Open your bot under `src/main/java/bots/<your-username>/`. Near the top:

```java
private static final double MOVE_DISTANCE = 100;
```

Change `100` to `20`. Recompile, run a battle, and watch how differently it behaves.
Then try `400`.

That is it. That is the whole loop. You just used a variable to change the behavior
of a program, which is the entire point of Chapter 2.

**Good first opponents:** `sample.SittingDuck` (cannot fight back), then
`sample.Corners`, then `sample.Crazy`. `sample.Walls` will beat you for a while.

---

## Write down the design

This repo has a `design.md`. Before you change the bot much, fill it in — a short Mermaid
class diagram of what is in here and what it does.

Two reasons. First, GitHub renders it, so anyone looking at your repo can see the shape of
your bot without reading code. Second, when your bot grows past two classes you will want
it, and the habit is easier to build now than later.

The course's UML materials have the tutorial and a cheat sheet.

---

## Make your own bot

`HornetBot` is the reference bot. You do not edit it — you make your own:

```
./tourney init <your-github-username> ThunderTank
```

On Windows use `tourney.cmd` instead of `./tourney`, here and everywhere below.

That creates `src/main/java/bots/<your-username>/ThunderTank.java`, a copy of the
reference bot in a package that is yours. Edit that file. Leave everyone else's folder
alone.

**Why the folder is named after you.** Every bot in a battle needs a name no one else
has. Asking people to pick unique class names does not work — four people will pick
`ThunderTank`. GitHub usernames are already unique, so using yours as the package name
makes collisions impossible. Your bot's real name is `bots.<your-username>.ThunderTank`,
and nobody else can have it.

---

## Battling other people

One command. No file swapping, no installing anyone's jar.

```
./tourney                     every bot in your copy of the repo
./tourney alice bob           just you against alice and bob
./tourney --list              who is currently in the repo
```

`./tourney` compiles every bot, packages each one, and runs the battle. It prints a
scoreboard when it is done:

```
  RANK  BOT                                  SCORE      ROUNDS WON
  ------------------------------------------------------------------
     1  octocat/ThunderTank                    498              4
     2  hornet/HornetBot                       219              1
```

### Getting your classmates' bots

Their bots arrive in your fork the same way any other change does — you pull them.

```
# Once, ever. The URL is on this repo's green "Code" button — the repo you
# forked FROM, not your own fork.
git remote add upstream https://github.com/DSU-CSCI-121-F26/robocode-arena.git

# Any time you want everyone's latest bots:
git pull upstream main
```

That is it. `git pull upstream main` brings down every bot that has been published
since you last looked, and `./tourney --list` shows you who is new.

### Publishing your own bot

Nobody can fight your bot until you publish it. Publishing is a pull request:

```
git switch -c my-bot
git add src/main/java/bots/<your-username>/
git commit -m "Add ThunderTank"
git push -u origin my-bot
```

Then open a pull request against this repo. **Only ever add or change files inside your
own folder** — that is what stops 25 pull requests from fighting each other, and CI will
reject the PR if you stray outside it.

Push an improved bot the same way, any time. There is no deadline and no limit.

> Your fork is where you experiment. Upstream is where you publish. Nobody sees your
> half-finished ideas unless you decide to send them.

---

## Options

| Flag | What it does |
|---|---|
| `--rounds N` | How many rounds per battle. Default 10. |
| `--all` | Every bot in the repo. This is also what you get with no arguments. |
| `--samples` | Add `sample.Corners`, `sample.Crazy`, and `sample.Walls` as extra opponents. Added automatically if you are the only bot. |
| `--pairwise` | Round robin — every bot fights every other bot one-on-one, ranked by matches won. Slower and much fairer than one big melee. |
| `--list` | Print the bots it can see and stop. |
| `--robocode <path>` | Where Robocode is installed, if not `~/robocode`. |
| `init <user> <BotName>` | Create your own bot folder. |

A bot that does not compile is reported and dropped from the battle. Everyone else's
bots still fight. You are never blocked by someone else's broken code.

---

## How battles actually get run

| Situation | How it works |
|---|---|
| **You, practising** | `./tourney`, or the Robocode GUI if you want to watch. Fight the `sample.*` bots with `--samples`. |
| **You vs a classmate** | `git pull upstream main`, then `./tourney <their-username>`. |
| **The whole class** | `./tourney --all --pairwise`. Runs on your machine, uses whatever bots you have pulled. |
| **In class** | Same command, projected. |
| **The tournament** | Same command again, on the instructor's machine, on the last published version of every bot. |

Nothing about the tournament is special — it is the command you have been running all
semester, with everybody's bot pulled in.

If you are curious what this looks like at scale, Robocode ships with
**RoboRumble** (`roborumble.sh`) — a distributed ranking system where bots from all
over the internet fight continuously. Not part of this course. Fun rabbit hole.

---

## Watching a battle instead of scoring it

`./tourney` runs Robocode with the display off, because a tournament that opens 40
windows is not a tournament. When you want to *watch* — and you should, it is the
whole point — use the Robocode GUI as set up above, or run a single battle and pay
attention to the scoreboard.

---

## One upgrade per week

Do these when the matching week comes up in class. Each one is small.

| Week | Upgrade to try |
|---|---|
| **2** — Variables and types | Change `MOVE_DISTANCE`, `DODGE_DISTANCE`, and `DODGE_TURN`. Find values that beat `sample.Corners`. |
| **3** — Your first class | Add a small class that remembers the last place you scanned an enemy. Give it fields and a constructor. |
| **4** — Objects interacting | Give your bot a colour scheme with `setColors()`. Then have your bot *ask* that memory class where to aim. |
| **5** — Methods | The `run()` loop is getting crowded. Pull the driving into a `patrol()` method and call it. |
| **6** — Branching, loops, **enums** | Only fire if the enemy is closer than 300 — save your energy. Then add `private enum Mode { PATROL, ENGAGE, EVADE }` and a field to hold the current one. Nothing has to use it yet. |
| **7** — **State machines** | Now use it. Wrap `run()` in `while (true) { switch (mode) { ... } }` and move between modes: patrol until you scan someone, engage until you get hit, evade, then back to patrol. **This is the single most useful thing in this list — see below.** |
| **9** — Arrays | Keep the last 10 places you scanned an enemy in an array. Are they circling you? |
| **10** — Inheritance | Change `extends Robot` to `extends AdvancedRobot` — non-blocking movement and much more control. Then split your bot into a base class and two subclasses with different personalities. |
| **11** — Polymorphism and interfaces | Make both subclasses satisfy one interface — say `Targeting` with a single `aimAt(ScannedRobotEvent)` method — and swap strategies without touching the bot. |
| **13** — Exceptions | What happens when your targeting math divides by zero because the enemy is exactly on top of you? Find out. Handle it. |

---

## Why this looks exactly like your term project

Not a coincidence, and worth seeing early.

Your tank and the maze rover you build with your group are **the same shape of program**:

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

Both extend a framework class you did not write. Both put everything in a method called
`run()`. Both loop until they are done. The rover's version simply has an **enum** and a
**switch** inside — which is exactly the week 7 upgrade above.

**That is why this repo is worth your time.** The rover is slow to experiment on: a
battery, a pairing, floor conditions, and three teammates' schedules per attempt. Your
tank recompiles in one second. Every state-machine mistake you are going to make — the
state that never transitions, the transition with no matching `case`, the state you set
but never act on — is cheaper to make here first.

> **One real difference, and it is worth understanding.** Robocode is *event-driven*: you
> write `onScannedRobot` and the framework calls it when something happens. The rover
> *polls*: your loop asks the sensors for a reading and then decides. Two ways to
> structure a control program, and you will have written both.

---

## About the tests

Look at `BotMath.java` and `BotMathTest.java`.

A `Robot` can only run inside a battle, which makes it painful to test. So the
calculations live in `BotMath` instead — plain static methods, no robot involved —
and JUnit checks them in milliseconds.

**This is the move you will make again on the term project**, and it is worth naming.
Split the *decision* from the *action*:

| Hard to test | Easy to test |
|---|---|
| `ahead(100)` — needs a battle | `firePowerFor(distance)` — needs nothing |
| `mbot.followLine()` — needs a rover, a floor, and a battery | *"given this colour label, which state comes next?"* — needs nothing |

The actions have to be tried on the real thing. The decisions do not, and the decisions
are where your bugs actually live. Pull them out into plain methods and you can test the
brain of your robot on a laptop at midnight with no hardware in the room.

**This is the trick.** Pull the thinking out of the framework so you can test the
thinking. You will do exactly this to your term project robot later in the semester.

When you add real logic to your bot — targeting, dodging, deciding when to fire —
put the calculation in `BotMath`, and write a test for it. Then you will know it
works before you ever start a battle.

Run the tests any time with `./mvnw test`. They also run automatically every time
you push, and you will see a green check or a red X on GitHub.

---

## The tournament

Last day of class. Whatever bot you have published upstream by then is the bot that
fights — so publish early, and publish again whenever you improve it.

The bracket is `./tourney --all --pairwise`: every bot against every other bot,
one-on-one, ranked by matches won.

Entering earns extra credit. Winning earns bragging rights, which last longer.

---

## Using AI on this

This repo is **Tier 1 — AI-assisted, with disclosure.** Use a model to explain
Robocode's API, to review your bot, or to help you debug. Add an `AI-USE.md` saying
what you asked for and what you changed.

The usual standard applies: if you cannot explain why your bot does what it does,
you do not have a bot, you have someone else's bot.

Also, a warning that will save you time — models confidently invent Robocode methods
that do not exist. When one tells you about `robocode.Robot#superAimBot()`, check the
[real API docs](https://robocode.sourceforge.io/docs/robocode/) before you go hunting
for why it will not compile.

---

## When something breaks

| Symptom | Fix |
|---|---|
| `./tourney` does not list your bot | Your class must be `public` and `extends Robot` (or `AdvancedRobot`), and live under `src/main/java/bots/<your-username>/`. Run `./tourney --list` to see what it can find |
| **The GUI** does not list your bot | In order: (1) run `./mvnw compile` — `clean` deletes `target/classes` and the bot with it, (2) press **Cmd+R** in the New Battle dialog, (3) check Development Options points at exactly `target/classes`, (4) look under `bots.<your-username>` |
| `./tourney` says Robocode is not installed | It looks in `~/robocode`. If yours is elsewhere: `./tourney --robocode /path/to/robocode`, or set `ROBOCODE_HOME` |
| A classmate's bot is missing from the battle | Either they have not published it yet, or you have not run `git pull upstream main`. If it failed to compile, `./tourney` says so and carries on without it |
| "Robocode" missing from the run dropdown | IntelliJ did not pick up `.run/`. File → Reload All from Disk, or re-open the project |
| `ClassNotFoundException: robocode.Robocode` | Maven has not reimported. Click the **Reload All Maven Projects** button in the Maven panel, then run again |
| Double-clicking `robocode.command` says `./robocode.sh: No such file or directory` | Robocode's shipped launcher does not `cd` first. Add `cd "$(dirname "$0")"` above the last line |
| Robocode starts then immediately dies | Working directory is not the Robocode install |
| Console says `Loading plugins from` the wrong folder | Working directory is pointing at the wrong place |
| `cannot find symbol: class Robot` | Run `./mvnw compile` once so Maven downloads the Robocode API |
| Bot appears but instantly dies | You probably have an exception in `run()`. Robocode shows it in the console — read the stack trace |
| Changes do not take effect | You edited, but did not recompile. `./mvnw compile` |
