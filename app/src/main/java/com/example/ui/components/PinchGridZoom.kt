package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Mounts a lag-free pinch-to-zoom detector directly on a grid.
 *
 * It only engages when two fingers are down, so single-finger scrolling and
 * tapping keep working normally. Once a pinch is active it consumes the
 * pointer events so the grid never scrolls mid-pinch, and every part of the
 * gesture is handled here (it does not depend on an outer wrapper).
 *
 * Pinch out (fingers spread) => fewer columns (bigger tiles).
 * Pinch in  (fingers close)  => more columns (smaller tiles).
 */
@Composable
fun Modifier.pinchGridZoom(
    currentColumns: Int,
    onColumnsChange: (Int) -> Unit,
    minColumns: Int = 2,
    maxColumns: Int = 5
): Modifier {
    val latestColumns by rememberUpdatedState(currentColumns)
    val latestOnColumnsChange by rememberUpdatedState(onColumnsChange)
    return this.pointerInput(Unit) {
        var cumulativeFactor = 1f
        var baseDist = 0f
        var lastCommitMs = 0L
        var active = false

        awaitEachGesture {
            cumulativeFactor = 1f
            baseDist = 0f
            active = false
            awaitFirstDown(requireUnconsumed = false)
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break

                if (pressed.size >= 2 && !active) {
                    // Pinch just started: take the current finger distance as the baseline.
                    active = true
                    baseDist = (pressed[0].position - pressed[1].position).getDistance().coerceAtLeast(1f)
                    cumulativeFactor = 1f
                }

                if (active) {
                    if (pressed.size >= 2) {
                        val dist = (pressed[0].position - pressed[1].position).getDistance().coerceAtLeast(1f)
                        cumulativeFactor = dist / baseDist
                        val now = SystemClock.uptimeMillis()
                        if (now - lastCommitMs >= 150L) {
                            val cols = latestColumns
                            val newCols = when {
                                cumulativeFactor > 1.3f -> (cols - 1).coerceIn(minColumns, maxColumns)
                                cumulativeFactor < 0.75f -> (cols + 1).coerceIn(minColumns, maxColumns)
                                else -> cols
                            }
                            if (newCols != cols) {
                                lastCommitMs = now
                                // Re-baseline so the same pinch can keep stepping.
                                baseDist = dist
                                cumulativeFactor = 1f
                                latestOnColumnsChange(newCols)
                            }
                        }
                    }
                    // Hold the grid still while the pinch is happening.
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}