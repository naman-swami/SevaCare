package com.sevacare.app.ui.navigator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard alias for LifeAdminNavigatorScreen.
 */
@Composable
fun LifeAdminScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LifeAdminNavigatorScreen(
        onBack = onBack,
        onSpeak = onSpeak,
        modifier = modifier
    )
}
