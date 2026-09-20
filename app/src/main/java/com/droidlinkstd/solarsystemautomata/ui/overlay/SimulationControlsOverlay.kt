package com.droidlinkstd.solarsystemautomata.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.droidlinkstd.solarsystemautomata.domain.physics.ScenarioPreset
import com.droidlinkstd.solarsystemautomata.domain.physics.ScenarioPresets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidlinkstd.solarsystemautomata.domain.physics.SimulationEngine
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * Lightweight Compose UI floating over the Canvas in a Box.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Recomposition Scope Isolation: Encapsulated in its own scope so user interactions
 *   do not trigger Canvas redraws or Recomposition cycles.
 * - Controls simulation loop (play/pause, speed multipliers, single stepping, camera reset).
 * - Displays real-time telemetry HUD (FPS, frame time, tracked body count).
 */
@Composable
fun SimulationControlsOverlay(
    simulationEngine: SimulationEngine,
    cameraState: CameraState,
    fps: Float,
    frameTimeMs: Float,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier,
    selectedPreset: ScenarioPreset = ScenarioPresets.SolarSystem,
    onSelectPreset: (ScenarioPreset) -> Unit = {}
) {
    val isRunning by simulationEngine.isRunningFlow.collectAsState()
    var currentSpeed by remember { mutableDoubleStateOf(simulationEngine.speedMultiplier) }
    var isPresetPickerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // --- Top Telemetry HUD ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // System info chip
                val currentAccent = Color(selectedPreset.accentColorHex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0B0F19))
                        .border(
                            1.dp,
                            currentAccent.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedPreset.iconEmoji,
                            fontSize = 12.sp
                        )
                        Text(
                            text = selectedPreset.title.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = currentAccent,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Diagnostics HUD (Compact single-chip telemetry to prevent narrow-screen wrapping)
                GlassChip(
                    text = "${simulationEngine.getRenderSnapshot().count} BODIES · ${fps.toInt()} FPS",
                    accentColor = if (fps >= 55f) Color(0xFF38BDF8) else Color(0xFFFBBF24)
                )
            }

            // Follow Mode Active Indicator
            AnimatedVisibility(
                visible = cameraState.isFollowing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                val snapshot = simulationEngine.getRenderSnapshot()
                val idx = cameraState.followedBodyIndex
                val targetName = if (idx in 0 until snapshot.count) {
                    val name = snapshot.names[idx]
                    if (name.isNotEmpty()) name else "BODY #$idx"
                } else {
                    "TARGET"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE0B132B))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Text(
                        text = "FOLLOWING: ${targetName.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE0F2FE),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "✕",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { cameraState.stopFollowing() }
                            .padding(horizontal = 4.dp)
                    )
                }
            }


        }

        // --- Bottom Control Deck ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xE60A0E18),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0x3360A5FA),
                        Color(0x111E293B)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Control Actions Row (Play/Pause & Fit All)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Redesigned Play / Pause Button with dynamic state gradient
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isRunning) {
                                    Brush.horizontalGradient(listOf(Color(0xFF1E40AF), Color(0xFF2563EB)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF065F46), Color(0xFF059669)))
                                }
                            )
                            .border(
                                1.dp,
                                if (isRunning) Color(0xFF60A5FA) else Color(0xFF34D399),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (isRunning) {
                                    simulationEngine.pause()
                                } else {
                                    simulationEngine.start()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isRunning) "⏸" else "▶",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isRunning) "PAUSE" else "PLAY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Sun-Centered Fit All Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(
                                1.dp,
                                Color(0xFF334155),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onResetCamera)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⤢",
                                fontSize = 14.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "FIT ALL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speed Multiplier header
                val formattedSpeed = when {
                    currentSpeed < 0.95 -> String.format(Locale.US, "%.2fx", currentSpeed)
                    currentSpeed < 9.95 -> String.format(Locale.US, "%.1fx", currentSpeed)
                    else -> String.format(Locale.US, "%.0fx", currentSpeed)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WARP SPEED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = formattedSpeed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF60A5FA)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Logarithmic Slider from 0.25x to 100x: maps [0f, 1f] via speed = 0.25 * 400^t
                val sliderPosition = (ln((currentSpeed / 0.25).coerceAtLeast(0.01)) / ln(400.0)).toFloat().coerceIn(0f, 1f)

                Slider(
                    value = sliderPosition,
                    onValueChange = { t ->
                        val raw = 0.25 * 400.0.pow(t.toDouble())
                        val cleanSpeed = when {
                            raw < 0.95 -> round(raw * 20.0) / 20.0 // 0.05 increments
                            raw < 4.8 -> round(raw * 10.0) / 10.0  // 0.1 increments
                            raw < 19.5 -> round(raw * 2.0) / 2.0   // 0.5 increments
                            else -> round(raw)                     // 1.0 increments
                        }.coerceIn(0.25, 100.0)

                        currentSpeed = cleanSpeed
                        simulationEngine.setSpeedMultiplier(cleanSpeed)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF60A5FA),
                        activeTrackColor = Color(0xFF3B82F6),
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Quick Tap Speed Preset Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.25 to "0.25x", 1.0 to "1x", 5.0 to "5x", 20.0 to "20x", 100.0 to "100x").forEach { (speedVal, label) ->
                        val isSelected = abs(currentSpeed - speedVal) < 0.06
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF64748B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0x333B82F6) else Color.Transparent)
                                .clickable {
                                    currentSpeed = speedVal
                                    simulationEngine.setSpeedMultiplier(speedVal)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassChip(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0B0F19))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE2E8F0),
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xFF1E293B))
            .border(
                1.dp,
                if (isActive) activeColor else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color(0xFFCBD5E1),
            letterSpacing = 0.5.sp
        )
    }
}
