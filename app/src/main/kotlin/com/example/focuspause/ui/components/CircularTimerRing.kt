/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults

@Composable
fun CircularTimerRing(
    progress: Float,
    isAmbient: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (isAmbient) 0 else 500),
        label = "timerProgressAnimation"
    )

    val indicatorColor = if (isAmbient) {
        Color.White.copy(alpha = 0.5f)
    } else {
        activeColor
    }

    val trackColor = if (isAmbient) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    CircularProgressIndicator(
        modifier = modifier
            .fillMaxSize()
            .padding(CircularProgressIndicatorDefaults.FullScreenPadding),
        progress = { animatedProgress },
        strokeWidth = if (isAmbient) 3.dp else 5.dp,
        colors = ProgressIndicatorDefaults.colors(
            indicatorColor = indicatorColor,
            trackColor = trackColor
        )
    )
}
