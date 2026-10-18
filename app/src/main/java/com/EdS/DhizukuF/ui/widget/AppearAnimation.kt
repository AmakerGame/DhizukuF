package com.EdS.DhizukuF.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Smooth fade + slide-up used for every card in the app (same feel as "User management").
 * Plays once per item (state survives scrolling out/in of a LazyColumn) and runs entirely in the
 * graphics layer, so it never triggers recomposition or re-layout while animating.
 */
@Composable
fun appearModifier(delayMillis: Int = 0): Modifier {
    var appeared by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (appeared) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!appeared) {
            progress.animateTo(
                1f,
                tween(durationMillis = 380, delayMillis = delayMillis, easing = FastOutSlowInEasing)
            )
            appeared = true
        }
    }
    return Modifier.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 28.dp.toPx()
    }
}
