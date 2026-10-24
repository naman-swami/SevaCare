package com.sevacare.app.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * StartupAnimationScreen alias invoking StartupWindowAnimation.
 */
@Composable
fun StartupAnimationScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    StartupWindowAnimation(
        onAnimationFinished = onAnimationFinished,
        modifier = modifier
    )
}
