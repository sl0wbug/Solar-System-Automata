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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.droidlinkstd.solarsystemautomata.ui.interaction.SlingshotState
import com.droidlinkstd.solarsystemautomata.ui.interaction.SpawnPreset

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
    slingshotState: SlingshotState = remember { SlingshotState() },
    selectedPreset: ScenarioPreset = ScenarioPresets.SolarSystem,
    onSelectPreset: (ScenarioPreset) -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(simulationEngine.isRunning) }
    var currentSpeed by remember { mutableDoubleStateOf(simulationEngine.speedMultiplier) }
    var isPresetPickerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
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
                // System info chip & Mode switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val currentAccent = Color(selectedPreset.accentColorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC0B0F19))
                            .border(
                                1.dp,
                                if (isPresetPickerOpen) currentAccent else currentAccent.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isPresetPickerOpen = !isPresetPickerOpen }
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
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isPresetPickerOpen) "▲" else "▼",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Spawn / Navigate Mode Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (slingshotState.isSpawnModeEnabled) Color(0xFFD97706) else Color(0xCC0B0F19)
                            )
                            .border(
                                1.dp,
                                if (slingshotState.isSpawnModeEnabled) Color(0xFFFBBF24) else Color(0x44F59E0B),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                slingshotState.isSpawnModeEnabled = !slingshotState.isSpawnModeEnabled
                                if (slingshotState.isSpawnModeEnabled) {
                                    cameraState.stopFollowing()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (slingshotState.isSpawnModeEnabled) "🚀 SPAWN" else "🔭 NAV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (slingshotState.isSpawnModeEnabled) Color.White else Color(0xFFFCD34D),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Diagnostics HUD (FPS & Frame Time)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        text = "${simulationEngine.getRenderSnapshot().count} BODIES",
                        accentColor = Color(0xFF34D399)
                    )
                    GlassChip(
                        text = "${fps.toInt()} FPS · ${String.format("%.1f", frameTimeMs)}ms",
                        accentColor = if (fps >= 55f) Color(0xFF38BDF8) else Color(0xFFFBBF24)
                    )
                }
            }

            // Scenario Presets Dropdown Picker
            AnimatedVisibility(
                visible = isPresetPickerOpen,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xF00F172A),
                                    Color(0xF0020617)
                                )
                            )
                        )
                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(320.dp)
                    ) {
                        Text(
                            text = "CELESTIAL PRESETS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )

                        ScenarioPresets.ALL_PRESETS.forEach { preset ->
                            val isCurrent = preset.id == selectedPreset.id
                            val accentColor = Color(preset.accentColorHex)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCurrent) accentColor.copy(alpha = 0.15f)
                                        else Color(0x331E293B)
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) accentColor else Color(0x22475569),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        isPresetPickerOpen = false
                                        onSelectPreset(preset)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = preset.iconEmoji,
                                        fontSize = 18.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = preset.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isCurrent) accentColor else Color.White
                                            )
                                            if (isCurrent) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = accentColor
                                                )
                                            }
                                        }
                                        Text(
                                            text = preset.subtitle,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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

            // Spawn Preset Selector Row (visible when Spawn Mode is active)
            AnimatedVisibility(
                visible = slingshotState.isSpawnModeEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xEE0B1120))
                        .border(1.dp, Color(0x44F59E0B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FLING:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF59E0B),
                        letterSpacing = 0.5.sp
                    )

                    SpawnPreset.entries.forEach { preset ->
                        val isSelected = slingshotState.selectedPreset == preset
                        val presetColor = Color(preset.colorHex)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) presetColor.copy(alpha = 0.35f) else Color(0xFF1E293B)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) presetColor else Color(0x22FFFFFF),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { slingshotState.selectedPreset = preset }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
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
                // Top control buttons row (Play/Pause, Step, Reset Camera)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Button
                    ControlButton(
                        text = if (isRunning) "⏸ PAUSE" else "▶ PLAY",
                        isActive = isRunning,
                        activeColor = Color(0xFF3B82F6),
                        onClick = {
                            if (isRunning) {
                                simulationEngine.pause()
                                isRunning = false
                            } else {
                                simulationEngine.start()
                                isRunning = true
                            }
                        }
                    )

                    // Single Step (visible when paused)
                    AnimatedVisibility(
                        visible = !isRunning,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ControlButton(
                            text = "⏭ STEP",
                            isActive = false,
                            activeColor = Color(0xFF8B5CF6),
                            onClick = {
                                simulationEngine.stepOnce()
                            }
                        )
                    }

                    // Reset / Fit Camera Button
                    ControlButton(
                        text = "⤢ FIT ALL",
                        isActive = false,
                        activeColor = Color(0xFF10B981),
                        onClick = onResetCamera
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speed Multiplier row
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
                        text = "${String.format("%.1f", currentSpeed)}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF60A5FA)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Speed Quick Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.25, 1.0, 5.0, 20.0, 100.0).forEach { speed ->
                        val isSelected = currentSpeed == speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B)
                                )
                                .clickable {
                                    simulationEngine.setSpeedMultiplier(speed)
                                    currentSpeed = speed
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed.toInt().let { if (it > 0) it else speed }}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
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
                letterSpacing = 0.5.sp
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
