package com.droidlinkstd.solarsystemautomata.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidlinkstd.solarsystemautomata.domain.physics.PlanetInfo
import com.droidlinkstd.solarsystemautomata.domain.physics.ScenarioPresets

/**
 * Top navigation bar providing quick toggle pills for the Sun Overview and all 8 major planets.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Glassmorphic sci-fi neon aesthetic with high visual contrast.
 * - Horizontal smooth scrolling with state indicator highlights.
 * - Non-blocking UI interactions without triggering canvas redraws.
 */
@Composable
fun PlanetNavigationBar(
    selectedPlanet: String?,
    onSelectPlanet: (String?) -> Unit,
    modifier: Modifier = Modifier,
    planets: List<PlanetInfo> = ScenarioPresets.getPlanetList(),
    onOpenPresetPicker: (() -> Unit)? = null,
    isTransitioning: Boolean = false
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Landing Page / Sun Overview Pill
        val isOverviewSelected = selectedPlanet == null
        val overviewBorderColor by animateColorAsState(
            targetValue = if (isOverviewSelected) Color(0xFFFFD700) else Color(0x33475569),
            animationSpec = tween(durationMillis = 250),
            label = "overviewBorder"
        )
        val overviewBgBrush = if (isOverviewSelected) {
            Brush.horizontalGradient(
                listOf(
                    Color(0x33F59E0B),
                    Color(0x22B45309)
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    Color(0xD90F172A),
                    Color(0xD90B0F19)
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(overviewBgBrush)
                .border(1.dp, overviewBorderColor, RoundedCornerShape(14.dp))
                .clickable(enabled = !isTransitioning) {
                    onSelectPlanet(null)
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "☀️",
                    fontSize = 12.sp
                )
                Column {
                    Text(
                        text = "SOLAR SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = if (isOverviewSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOverviewSelected) Color(0xFFFEF08A) else Color(0xFFCBD5E1),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "OVERVIEW",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOverviewSelected) Color(0xFFFBBF24) else Color(0xFF64748B)
                    )
                }
            }
        }

        // 2. Planet Subsystem Navigation Pills
        for (planet in planets) {
            val isSelected = selectedPlanet.equals(planet.name, ignoreCase = true)
            val planetColor = Color(planet.colorHex)

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) planetColor else Color(0x26475569),
                animationSpec = tween(durationMillis = 250),
                label = "planetBorder"
            )

            val bgBrush = if (isSelected) {
                Brush.horizontalGradient(
                    listOf(
                        planetColor.copy(alpha = 0.28f),
                        planetColor.copy(alpha = 0.12f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xD90B0F19),
                        Color(0xD9070A12)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgBrush)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isTransitioning) {
                        onSelectPlanet(planet.name)
                    }
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Planet glyph / colored indicator
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(planetColor.copy(alpha = if (isSelected) 0.35f else 0.18f))
                            .border(1.dp, planetColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = planet.symbol,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = planetColor
                        )
                    }

                    Column {
                        Text(
                            text = planet.name.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            letterSpacing = 0.5.sp
                        )

                        val moonLabel = when (planet.moonCount) {
                            0 -> "NO MOONS"
                            1 -> "1 MOON"
                            else -> "${planet.moonCount} MOONS"
                        }
                        Text(
                            text = moonLabel,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) planetColor else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // 3. Scenario Presets Switcher Button
        if (onOpenPresetPicker != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD91E293B))
                    .border(1.dp, Color(0x4464748B), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenPresetPicker)
                    .padding(horizontal = 11.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "🌌",
                        fontSize = 11.sp
                    )
                    Text(
                        text = "PRESETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
