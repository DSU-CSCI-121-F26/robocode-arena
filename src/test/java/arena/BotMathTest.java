package arena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BotMath}.
 *
 * Read these before you write your own. Notice that every test does three things:
 * set up a situation, run one method, then check exactly one idea about the result.
 */
class BotMathTest {

    /** Doubles are approximate, so comparisons need a tolerance. */
    private static final double TOLERANCE = 0.0001;

    @Test
    @DisplayName("an angle already in range is left alone")
    void normalizeBearingLeavesInRangeAnglesAlone() {
        assertEquals(0, BotMath.normalizeBearing(0), TOLERANCE);
        assertEquals(90, BotMath.normalizeBearing(90), TOLERANCE);
        assertEquals(-90, BotMath.normalizeBearing(-90), TOLERANCE);
        assertEquals(180, BotMath.normalizeBearing(180), TOLERANCE);
    }

    @Test
    @DisplayName("a large angle wraps back into range")
    void normalizeBearingWrapsLargeAngles() {
        assertEquals(-10, BotMath.normalizeBearing(350), TOLERANCE);
        assertEquals(10, BotMath.normalizeBearing(370), TOLERANCE);
        assertEquals(0, BotMath.normalizeBearing(720), TOLERANCE);
        assertEquals(170, BotMath.normalizeBearing(-190), TOLERANCE);
    }

    @Test
    @DisplayName("clamp pulls values inside the range and leaves the rest")
    void clampRespectsBounds() {
        assertEquals(5, BotMath.clamp(5, 0, 10), TOLERANCE);
        assertEquals(0, BotMath.clamp(-3, 0, 10), TOLERANCE);
        assertEquals(10, BotMath.clamp(99, 0, 10), TOLERANCE);
    }

    @Test
    @DisplayName("close targets get a full-power shot")
    void firePowerIsMaxUpClose() {
        assertEquals(BotMath.MAX_FIRE_POWER, BotMath.firePowerFor(0), TOLERANCE);
        assertEquals(BotMath.MAX_FIRE_POWER, BotMath.firePowerFor(100), TOLERANCE);
    }

    @Test
    @DisplayName("distant targets get a weak shot")
    void firePowerIsLowFarAway() {
        assertEquals(1.0, BotMath.firePowerFor(500), TOLERANCE);
        assertEquals(1.0, BotMath.firePowerFor(2000), TOLERANCE);
    }

    @Test
    @DisplayName("fire power slides evenly between the two extremes")
    void firePowerScalesInBetween() {
        assertEquals(2.0, BotMath.firePowerFor(300), TOLERANCE);
    }

    @Test
    @DisplayName("fire power is never outside what Robocode allows")
    void firePowerAlwaysLegal() {
        for (double distance = -50; distance <= 1000; distance += 25) {
            double power = BotMath.firePowerFor(distance);
            assertTrue(power >= BotMath.MIN_FIRE_POWER,
                    "power too low at distance " + distance + ": " + power);
            assertTrue(power <= BotMath.MAX_FIRE_POWER,
                    "power too high at distance " + distance + ": " + power);
        }
    }
}
