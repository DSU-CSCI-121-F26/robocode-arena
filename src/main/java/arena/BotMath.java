package arena;

/**
 * Pure calculations your bot needs, kept separate from the bot itself.
 *
 * Why does this class exist? A Robot can only run inside a battle, so it is very
 * hard to test directly. These methods are plain static methods with no robot in
 * sight, which means JUnit can check them in milliseconds — no battle required.
 *
 * Pulling logic out of a framework so you can test it is one of the most useful
 * habits in professional software. You will do the same thing to the term project.
 */
public class BotMath {

    /** Smallest fire power Robocode allows. */
    public static final double MIN_FIRE_POWER = 0.1;

    /** Largest fire power Robocode allows. */
    public static final double MAX_FIRE_POWER = 3.0;

    /**
     * Wraps an angle so it lands between -180 and 180 degrees.
     *
     * Robocode gives you bearings that can be outside that range, and turning
     * 350 degrees left is a slow way to turn 10 degrees right.
     *
     * @param degrees any angle, in degrees
     * @return the same direction, expressed between -180 and 180
     */
    public static double normalizeBearing(double degrees) {
        while (degrees > 180) {
            degrees -= 360;
        }
        while (degrees <= -180) {
            degrees += 360;
        }
        return degrees;
    }

    /**
     * Forces a value to stay inside a range.
     *
     * @param value the value to limit
     * @param low   smallest allowed result
     * @param high  largest allowed result
     * @return value, moved inside [low, high] if it was outside
     */
    public static double clamp(double value, double low, double high) {
        if (value < low) {
            return low;
        }
        if (value > high) {
            return high;
        }
        return value;
    }

    /**
     * Picks how hard to shoot based on how far away the target is.
     *
     * Powerful shots are slow and cost more energy, so they are worth it up close
     * and wasteful far away. Anything closer than 100 gets a full-power shot;
     * anything past 500 gets the weakest useful shot; in between it slides evenly.
     *
     * @param distance distance to the enemy, in pixels
     * @return a fire power between {@link #MIN_FIRE_POWER} and {@link #MAX_FIRE_POWER}
     */
    public static double firePowerFor(double distance) {
        double power;
        if (distance <= 100) {
            power = MAX_FIRE_POWER;
        } else if (distance >= 500) {
            power = 1.0;
        } else {
            power = MAX_FIRE_POWER - (distance - 100) * 2.0 / 400.0;
        }
        return clamp(power, MIN_FIRE_POWER, MAX_FIRE_POWER);
    }
}
