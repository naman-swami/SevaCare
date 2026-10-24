package com.saathi.app.ui.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

/**
 * High-performance, deterministic vector path parser and tracer for Shield and 'S' monogram.
 * Works seamlessly in both JVM unit tests and Android Jetpack Compose Canvas render loops.
 */
class ShieldPathTracer(val pathString: String) {

    sealed class Segment {
        abstract val length: Float
        abstract fun pointAt(t: Float): Offset
        abstract fun tangentAt(t: Float): Offset

        data class Line(val start: Offset, val end: Offset) : Segment() {
            override val length: Float = (end - start).getDistance()
            override fun pointAt(t: Float): Offset = start + (end - start) * t
            override fun tangentAt(t: Float): Offset {
                val d = end - start
                val len = length.coerceAtLeast(0.0001f)
                return Offset(d.x / len, d.y / len)
            }
        }

        data class Cubic(val p0: Offset, val p1: Offset, val p2: Offset, val p3: Offset) : Segment() {
            // Approximate length using 16 sub-segments
            private val samplePoints: List<Offset> = List(17) { i -> eval(i / 16f) }
            override val length: Float = (0 until 16).sumOf { i ->
                (samplePoints[i + 1] - samplePoints[i]).getDistance().toDouble()
            }.toFloat()

            private fun eval(t: Float): Offset {
                val u = 1f - t
                val tt = t * t
                val uu = u * u
                val uuu = uu * u
                val ttt = tt * t

                val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
                val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
                return Offset(x, y)
            }

            override fun pointAt(t: Float): Offset = eval(t.coerceIn(0f, 1f))

            override fun tangentAt(t: Float): Offset {
                val u = 1f - t
                val dx = 3 * u * u * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * t * t * (p3.x - p2.x)
                val dy = 3 * u * u * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * t * t * (p3.y - p2.y)
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
                return Offset(dx / len, dy / len)
            }
        }
    }

    val segments = mutableListOf<Segment>()
    val length: Float

    init {
        var current = Offset.Zero
        var subpathStart = Offset.Zero

        // Tokenize SVG path string
        val tokens = tokenizePath(pathString)
        var i = 0
        while (i < tokens.size) {
            val cmd = tokens[i++]
            when (cmd) {
                "M" -> {
                    val x = tokens[i++].toFloat()
                    val y = tokens[i++].toFloat()
                    current = Offset(x, y)
                    subpathStart = current
                }
                "L" -> {
                    val x = tokens[i++].toFloat()
                    val y = tokens[i++].toFloat()
                    val next = Offset(x, y)
                    segments.add(Segment.Line(current, next))
                    current = next
                }
                "V" -> {
                    val y = tokens[i++].toFloat()
                    val next = Offset(current.x, y)
                    segments.add(Segment.Line(current, next))
                    current = next
                }
                "H" -> {
                    val x = tokens[i++].toFloat()
                    val next = Offset(x, current.y)
                    segments.add(Segment.Line(current, next))
                    current = next
                }
                "C" -> {
                    val p1 = Offset(tokens[i++].toFloat(), tokens[i++].toFloat())
                    val p2 = Offset(tokens[i++].toFloat(), tokens[i++].toFloat())
                    val p3 = Offset(tokens[i++].toFloat(), tokens[i++].toFloat())
                    segments.add(Segment.Cubic(current, p1, p2, p3))
                    current = p3
                }
                "Z", "z" -> {
                    if (current != subpathStart) {
                        segments.add(Segment.Line(current, subpathStart))
                        current = subpathStart
                    }
                }
            }
        }
        length = segments.sumOf { it.length.toDouble() }.toFloat()
    }

    private fun tokenizePath(d: String): List<String> {
        val result = mutableListOf<String>()
        val regex = Regex("([MLVHCZmlvhcz])|([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
        for (match in regex.findAll(d)) {
            result.add(match.value)
        }
        return result
    }

    fun getPosTan(distance: Float, pos: FloatArray, tan: FloatArray) {
        if (segments.isEmpty() || length <= 0f) {
            pos[0] = 0f; pos[1] = 0f
            tan[0] = 1f; tan[1] = 0f
            return
        }

        val clampedDist = distance.coerceIn(0f, length)
        var accumulated = 0f

        for (seg in segments) {
            val segLen = seg.length
            if (accumulated + segLen >= clampedDist || seg == segments.last()) {
                val t = if (segLen > 0.0001f) (clampedDist - accumulated) / segLen else 0f
                val pt = seg.pointAt(t.coerceIn(0f, 1f))
                val tg = seg.tangentAt(t.coerceIn(0f, 1f))
                pos[0] = pt.x
                pos[1] = pt.y
                tan[0] = tg.x
                tan[1] = tg.y
                return
            }
            accumulated += segLen
        }

        val lastPt = segments.last().pointAt(1f)
        val lastTan = segments.last().tangentAt(1f)
        pos[0] = lastPt.x
        pos[1] = lastPt.y
        tan[0] = lastTan.x
        tan[1] = lastTan.y
    }

    fun toComposePath(scaleX: Float = 1f, scaleY: Float = 1f, offsetX: Float = 0f, offsetY: Float = 0f): Path {
        val path = Path()
        if (segments.isEmpty()) return path

        for ((index, seg) in segments.withIndex()) {
            when (seg) {
                is Segment.Line -> {
                    if (index == 0) {
                        path.moveTo(seg.start.x * scaleX + offsetX, seg.start.y * scaleY + offsetY)
                    }
                    path.lineTo(seg.end.x * scaleX + offsetX, seg.end.y * scaleY + offsetY)
                }
                is Segment.Cubic -> {
                    if (index == 0) {
                        path.moveTo(seg.p0.x * scaleX + offsetX, seg.p0.y * scaleY + offsetY)
                    }
                    path.cubicTo(
                        seg.p1.x * scaleX + offsetX, seg.p1.y * scaleY + offsetY,
                        seg.p2.x * scaleX + offsetX, seg.p2.y * scaleY + offsetY,
                        seg.p3.x * scaleX + offsetX, seg.p3.y * scaleY + offsetY
                    )
                }
            }
        }
        return path
    }

    fun getSegment(
        startDist: Float,
        endDist: Float,
        dst: Path,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        dst.reset()
        if (segments.isEmpty() || length <= 0f) return

        val sDist = startDist.coerceIn(0f, length)
        val eDist = endDist.coerceIn(sDist, length)
        if (sDist >= eDist) return

        var accumulated = 0f
        var first = true

        for (seg in segments) {
            val segLen = seg.length
            val segStart = accumulated
            val segEnd = accumulated + segLen

            if (segEnd >= sDist && segStart <= eDist) {
                val t0 = ((sDist - segStart) / segLen).coerceIn(0f, 1f)
                val t1 = ((eDist - segStart) / segLen).coerceIn(0f, 1f)

                when (seg) {
                    is Segment.Line -> {
                        val pA = seg.pointAt(t0)
                        val pB = seg.pointAt(t1)
                        if (first) {
                            dst.moveTo(pA.x * scaleX + offsetX, pA.y * scaleY + offsetY)
                            first = false
                        }
                        dst.lineTo(pB.x * scaleX + offsetX, pB.y * scaleY + offsetY)
                    }
                    is Segment.Cubic -> {
                        // Subdivide cubic or approximate with points
                        val steps = 8
                        for (step in 0..steps) {
                            val subT = t0 + (t1 - t0) * (step / steps.toFloat())
                            val pt = seg.pointAt(subT)
                            if (first) {
                                dst.moveTo(pt.x * scaleX + offsetX, pt.y * scaleY + offsetY)
                                first = false
                            } else {
                                dst.lineTo(pt.x * scaleX + offsetX, pt.y * scaleY + offsetY)
                            }
                        }
                    }
                }
            }
            accumulated += segLen
        }
    }
}
