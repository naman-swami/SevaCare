package com.saathi.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.R
import com.saathi.app.ui.theme.LocalReducedMotion
import com.saathi.app.ui.theme.LocalSevaColors
import com.saathi.app.ui.theme.LocalSevaHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time particle representation for Sparkle effects.
 */
data class SparkleParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val radius: Float
)

/**
 * Flagship Production-Grade Startup Animation for SevaCare.
 *
 * Choreography:
 * Stage A (0–800ms): Shield boundary path draws with real-time sparkle particles at tip.
 * Stage B (800–1600ms): Small yellow circle traces the exact vector path of "S" (cream stroke fills behind it).
 * Stage C (1600–2200ms): Complete logo rises from center to upper-third center (TRUE horizontal centering) and scales 1.15x -> 1.0x.
 * Stage D (2200–2600ms): App Name fades in below with alpha + translation settle.
 * Stage E (2600ms+): Tagline reveals character-by-character (~45ms/char) with blinking cursor, holds 900ms, then navigates to Home.
 */
@Composable
fun StartupWindowAnimation(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSevaColors.current
    val haptics = LocalSevaHaptics.current
    val isReducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val riseTargetY = -(screenHeightPx * 0.18f)

    // Stage progression
    var currentStage by remember { mutableIntStateOf(if (isReducedMotion) 5 else 1) }

    // Animatable timeline values
    val shieldProgress = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val sProgress = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 1.15f) }
    val logoTranslationY = remember { Animatable(if (isReducedMotion) riseTargetY else 0f) }
    val shieldFillAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val appNameAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val appNameTranslationY = remember { Animatable(if (isReducedMotion) 0f else 18f) }

    // Tagline typing state
    val fullTagline = stringResource(R.string.tagline)
    var typedTaglineLength by remember { mutableIntStateOf(if (isReducedMotion) fullTagline.length else 0) }
    var isCursorVisible by remember { mutableStateOf(true) }

    // Pre-parse vector paths
    val shieldTracer = remember { ShieldPathTracer(ShieldPaths.SHIELD_OUTLINE) }
    val sTracer = remember { ShieldPathTracer(ShieldPaths.S_LETTER) }

    // Particle system state
    val particles = remember { ArrayDeque<SparkleParticle>() }
    var particleTick by remember { mutableIntStateOf(0) }
    var lastNanos by remember { mutableLongStateOf(0L) }
    var activeEmitterPos by remember { mutableStateOf<Offset?>(null) }

    // Particle physics loop
    LaunchedEffect(Unit) {
        if (isReducedMotion) return@LaunchedEffect
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = now

                // Spawn particles at tip if emitter is active
                activeEmitterPos?.let { emitter ->
                    repeat(2) {
                        if (particles.size < 75) {
                            val angle = Random.nextFloat() * 2f * PI.toFloat()
                            val speed = 30f + Random.nextFloat() * 90f
                            particles.addLast(
                                SparkleParticle(
                                    x = emitter.x,
                                    y = emitter.y,
                                    vx = cos(angle) * speed,
                                    vy = sin(angle) * speed,
                                    life = 0f,
                                    maxLife = 0.45f + Random.nextFloat() * 0.35f,
                                    color = if (Random.nextBoolean()) Color(0xFFF5A623) else Color(0xFFFFD580),
                                    radius = 2f + Random.nextFloat() * 2.5f
                                )
                            )
                        }
                    }
                }

                // Step & prune particles
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.vy += 45f * dt // gentle gravity
                    p.vx *= (1f - 0.4f * dt) // drag
                    p.life += dt
                    if (p.life >= p.maxLife) {
                        iterator.remove()
                    }
                }
                particleTick++
            }
        }
    }

    // Sequenced Timeline Execution
    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            delay(1000)
            onAnimationFinished()
            return@LaunchedEffect
        }

        // ==========================================
        // Stage A: Shield Draw (0–800ms)
        // ==========================================
        currentStage = 1
        shieldProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        activeEmitterPos = null
        haptics.tick()

        // ==========================================
        // Stage B: 'S' Path Tracer (800–1600ms)
        // ==========================================
        currentStage = 2
        launch {
            shieldFillAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = LinearEasing)
            )
        }
        sProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        activeEmitterPos = null
        haptics.tick()

        // ==========================================
        // Stage C: Logo Rise & Scale (1600–2200ms)
        // ==========================================
        currentStage = 3
        launch {
            logoTranslationY.animateTo(
                targetValue = riseTargetY,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )

        // ==========================================
        // Stage D: App Name Reveal (2200–2600ms)
        // ==========================================
        currentStage = 4
        launch {
            appNameTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            )
        }
        appNameAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
        )

        // ==========================================
        // Stage E: Tagline Typing (2600ms+)
        // ==========================================
        currentStage = 5
        // Cursor blink loop
        val cursorJob = launch {
            while (true) {
                delay(350)
                isCursorVisible = !isCursorVisible
            }
        }

        for (i in 1..fullTagline.length) {
            typedTaglineLength = i
            delay(45)
        }

        haptics.confirm()
        delay(900)
        cursorJob.cancel()
        onAnimationFinished()
    }

    // Outer Background Box
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.canvas,
                        Color(0xFFFFF9EE),
                        colors.canvas
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Logo Composition — strictly horizontally centered!
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = logoTranslationY.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
                .width(190.dp)
                .height(208.dp),
            contentAlignment = Alignment.Center
        ) {
            val dstShield = remember { Path() }
            val dstS = remember { Path() }
            val posBuffer = remember { FloatArray(2) }
            val tanBuffer = remember { FloatArray(2) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 100f
                val scaleY = size.height / 100f

                // 1. Shield Background Fill
                if (shieldFillAlpha.value > 0f) {
                    val fullShieldPath = shieldTracer.toComposePath(scaleX = scaleX, scaleY = scaleY)
                    drawPath(
                        path = fullShieldPath,
                        color = Color(0xFF0F6B5C).copy(alpha = shieldFillAlpha.value)
                    )
                }

                // 2. Shield Boundary Draw (Stage A)
                if (shieldProgress.value > 0f) {
                    shieldTracer.getSegment(
                        startDist = 0f,
                        endDist = shieldTracer.length * shieldProgress.value,
                        dst = dstShield,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )

                    drawPath(
                        path = dstShield,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF168E7B),
                                Color(0xFF0F6B5C)
                            )
                        ),
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Update sparkle emitter position at the tip during Stage A
                    if (shieldProgress.value < 1f) {
                        shieldTracer.getPosTan(
                            distance = shieldTracer.length * shieldProgress.value,
                            pos = posBuffer,
                            tan = tanBuffer
                        )
                        activeEmitterPos = Offset(posBuffer[0] * scaleX, posBuffer[1] * scaleY)
                    }
                }

                // 3. 'S' Letter Path Draw (Stage B)
                if (sProgress.value > 0f) {
                    sTracer.getSegment(
                        startDist = 0f,
                        endDist = sTracer.length * sProgress.value,
                        dst = dstS,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )

                    // Cream/Off-white "S" stroke matching Shield Logo
                    drawPath(
                        path = dstS,
                        color = Color(0xFFFFF8EC),
                        style = Stroke(
                            width = 8.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Small yellow/saffron tracer circle traveling along path
                    sTracer.getPosTan(
                        distance = sTracer.length * sProgress.value,
                        pos = posBuffer,
                        tan = tanBuffer
                    )
                    val tipX = posBuffer[0] * scaleX
                    val tipY = posBuffer[1] * scaleY
                    val tracerCenter = Offset(tipX, tipY)

                    // Outer soft radiant glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF5A623).copy(alpha = 0.45f),
                                Color(0x00F5A623)
                            ),
                            center = tracerCenter,
                            radius = 16.dp.toPx()
                        ),
                        radius = 16.dp.toPx(),
                        center = tracerCenter
                    )

                    // Solid yellow tracer dot (radius ~7dp as specified)
                    drawCircle(
                        color = Color(0xFFF5A623),
                        radius = 7.dp.toPx(),
                        center = tracerCenter
                    )

                    // White inner core highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.5.dp.toPx(),
                        center = tracerCenter
                    )

                    // Update sparkle emitter position during Stage B
                    if (sProgress.value < 1f) {
                        activeEmitterPos = tracerCenter
                    }
                }

                // 4. Sparkle Particles System (Reading particleTick forces per-frame Canvas invalidation)
                @Suppress("UNUSED_EXPRESSION") particleTick
                particles.forEach { p ->
                    val alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.radius * alpha,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }

        // Stage D & E: App Name and Tagline below the logo
        if (appNameAlpha.value > 0f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 90.dp) // Below the raised logo
                    .graphicsLayer {
                        alpha = appNameAlpha.value
                        translationY = appNameTranslationY.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Name: SevaCare
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Seva",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.teal,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Care",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF5A623),
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline typing character-by-character with blinking cursor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val displayedTagline = fullTagline.take(typedTaglineLength)
                    Text(
                        text = displayedTagline,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )

                    if (typedTaglineLength < fullTagline.length || isCursorVisible) {
                        Canvas(
                            modifier = Modifier
                                .padding(start = 3.dp)
                                .size(width = 2.5.dp, height = 16.dp)
                        ) {
                            if (isCursorVisible) {
                                drawRect(color = Color(0xFFF5A623))
                            }
                        }
                    }
                }
            }
        }
    }
}
