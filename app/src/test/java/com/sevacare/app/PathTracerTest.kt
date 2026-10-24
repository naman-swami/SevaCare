package com.sevacare.app

import com.sevacare.app.ui.splash.ShieldPathTracer
import com.sevacare.app.ui.splash.ShieldPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CI Unit Test verifying vector path parsing, PathMeasure mathematics,
 * and endpoint trajectory accuracy for the Shield Logo and 'S' monogram.
 */
class PathTracerTest {

    @Test
    fun testSLetterPath_ParsesAndMatchesEndpoints() {
        val tracer = ShieldPathTracer(ShieldPaths.S_LETTER)

        // 1. Verify parsing produced segments and positive length
        assertTrue("Segments should not be empty", tracer.segments.isNotEmpty())
        assertTrue("Path length must be greater than 0", tracer.length > 50f)

        // 2. Sample at t=0 (start of path)
        val posStart = FloatArray(2)
        val tanStart = FloatArray(2)
        tracer.getPosTan(0f, posStart, tanStart)

        // Expected start from "M68,32": (68f, 32f)
        assertEquals("Start X should match M68", 68f, posStart[0], 0.5f)
        assertEquals("Start Y should match 32", 32f, posStart[1], 0.5f)

        // 3. Sample at t=1 (end of path)
        val posEnd = FloatArray(2)
        val tanEnd = FloatArray(2)
        tracer.getPosTan(tracer.length, posEnd, tanEnd)

        // Expected end from "34,70": (34f, 70f)
        assertEquals("End X should match tail 34", 34f, posEnd[0], 0.5f)
        assertEquals("End Y should match tail 70", 70f, posEnd[1], 0.5f)

        // 4. Sample mid-point trajectory
        val posMid = FloatArray(2)
        val tanMid = FloatArray(2)
        tracer.getPosTan(tracer.length * 0.5f, posMid, tanMid)
        assertTrue("Midpoint X must be in valid bounding box", posMid[0] in 20f..80f)
        assertTrue("Midpoint Y must be in valid bounding box", posMid[1] in 25f..75f)
    }

    @Test
    fun testShieldOutlinePath_ParsesAndHasClosedBounds() {
        val tracer = ShieldPathTracer(ShieldPaths.SHIELD_OUTLINE)

        assertTrue("Shield segments should not be empty", tracer.segments.isNotEmpty())
        assertTrue("Shield length must be greater than 100", tracer.length > 100f)

        val posStart = FloatArray(2)
        val tanStart = FloatArray(2)
        tracer.getPosTan(0f, posStart, tanStart)
        assertEquals("Shield top apex X", 50f, posStart[0], 0.5f)
        assertEquals("Shield top apex Y", 4f, posStart[1], 0.5f)

        val posEnd = FloatArray(2)
        val tanEnd = FloatArray(2)
        tracer.getPosTan(tracer.length, posEnd, tanEnd)
        // Since it's closed back to apex, end position should be near (50, 4)
        assertEquals("Shield closed apex X", 50f, posEnd[0], 1.0f)
        assertEquals("Shield closed apex Y", 4f, posEnd[1], 1.0f)
    }
}
