package us.dot.its.jpo.conflictmonitor.monitor.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class MathFunctionsTest {

    @Test
    public void testGetMedianHeadingEmptyList() {
        assertEquals(0.0, MathFunctions.getMedianHeading(new ArrayList<>()), 0.0001);
    }

    @Test
    public void testGetMedianHeadingNoWraparound() {
        // Sanity check: away from the 0/360 boundary, the circular median should match
        // the plain numeric median.
        ArrayList<Double> headings = new ArrayList<>(Arrays.asList(100.0, 105.0, 110.0, 120.0));
        double expected = MathFunctions.getMedian(new ArrayList<>(headings));
        assertEquals(expected, MathFunctions.getMedianHeading(headings), 0.0001);
    }

    @Test
    public void testGetMedianHeadingOddCountWraparound() {
        // True center is 0/360. A naive numeric median of [359, 0, 1] would sort correctly
        // by luck here (0 sits in the middle), but headings straddle the wrap boundary.
        ArrayList<Double> headings = new ArrayList<>(Arrays.asList(359.0, 0.0, 1.0));
        assertEquals(0.0, MathFunctions.getMedianHeading(headings), 0.0001);
    }

    @Test
    public void testGetMedianHeadingEvenCountWraparoundRegression() {
        // Regression test for the false-positive Lane Direction of Travel bug: vehicle
        // headings scattered across the 0/360 boundary but clustered around a true
        // heading of ~0 degrees. The old MathFunctions.getMedian() implementation sorts
        // these numerically to [1, 2, 358, 359] and averages the two middle values,
        // producing a median of 180 degrees -- exactly 180 degrees away from the true
        // heading, and the symptom reported in production. getMedianHeading() must not
        // reproduce that error.
        ArrayList<Double> headings = new ArrayList<>(Arrays.asList(358.0, 359.0, 1.0, 2.0));

        double naiveMedian = MathFunctions.getMedian(new ArrayList<>(headings));
        assertEquals("sanity check: the naive median exhibits the 180 degree bug", 180.0, naiveMedian, 0.0001);

        double circularMedian = MathFunctions.getMedianHeading(headings);
        assertEquals(0.0, circularMedian, 0.0001);
    }

    @Test
    public void testGetMedianHeadingRealisticScatterAroundZero() {
        // A wider, realistic scatter of GPS/heading noise straddling the wrap boundary,
        // still clustered around a true heading of ~0 degrees.
        ArrayList<Double> headings = new ArrayList<>(Arrays.asList(355.0, 358.0, 2.0, 5.0, 359.0, 1.0));

        double naiveMedian = MathFunctions.getMedian(new ArrayList<>(headings));
        assertEquals("sanity check: the naive median exhibits the 180 degree bug", 180.0, naiveMedian, 0.0001);

        double circularMedian = MathFunctions.getMedianHeading(headings);
        assertEquals(0.0, circularMedian, 0.0001);
    }

    @Test
    public void testGetMedianHeadingWraparoundNearOppositeBoundary() {
        // Same wraparound failure mode, but clustered around 180 degrees where the plain
        // numeric median is actually correct -- used to confirm getMedianHeading() doesn't
        // introduce error away from the 0/360 boundary.
        ArrayList<Double> headings = new ArrayList<>(Arrays.asList(170.0, 175.0, 185.0, 190.0));
        assertEquals(180.0, MathFunctions.getMedianHeading(headings), 0.0001);
    }
}
