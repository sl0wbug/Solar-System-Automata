package com.droidlinkstd.solarsystemautomata.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import java.util.Locale
import kotlin.math.hypot

/**
 * Floating glassmorphic telemetry inspector card displaying real-time physical metrics
 * and action controls for a selected celestial body.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Reads telemetry directly from [RenderSnapshot] SoA primitive buffers without thread locks.
 * - Zero allocations during recomposition / property extraction.
 */
@Composable
fun BodyInspectorCard(
    snapshot: RenderSnapshot,
    bodyIndex: Int,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onDeleteBody: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bodyIndex !in 0 until snapshot.count) return

    val name = snapshot.names[bodyIndex].ifEmpty { "BODY #$bodyIndex" }
    val colorHex = snapshot.color[bodyIndex]
    val bodyColor = Color(colorHex)

    val x = snapshot.posX[bodyIndex]
    val y = snapshot.posY[bodyIndex]
    val vx = snapshot.velX[bodyIndex]
    val vy = snapshot.velY[bodyIndex]
    val mass = snapshot.mass[bodyIndex]
    val radius = snapshot.radius[bodyIndex]

    val speed = hypot(vx, vy)
    val distance = hypot(x, y)

    val isMoon = radius < 0.5f && mass < 0.05
    val (classification, classColor) = when {
        mass >= 1000.0 -> "STAR" to Color(0xFFFBBF24)
        mass >= 10.0 -> "GAS GIANT" to Color(0xFFFB923C)
        isMoon -> "MOON" to Color(0xFFA78BFA)
        mass >= 0.05 -> "TERRESTRIAL" to Color(0xFF38BDF8)
        else -> "ASTEROID" to Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00F172A),
                        Color(0xF0020617)
                    )
                )
            )
            .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Color dot, Name, Classification Chip, Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(bodyColor)
                    )
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(classColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = classification,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = classColor
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0x33475569))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Telemetry Grid (2x2 key-value metrics)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x331E293B))
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TelemetryMetricItem(
                        label = "VELOCITY",
                        value = String.format(Locale.US, "%.2f AU/s", speed)
                    )
                    TelemetryMetricItem(
                        label = "DISTANCE",
                        value = String.format(Locale.US, "%.2f AU", distance)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TelemetryMetricItem(
                        label = "MASS",
                        value = if (mass >= 1000.0) {
                            String.format(Locale.US, "%.0f M⊕", mass)
                        } else {
                            String.format(Locale.US, "%.2f M⊕", mass)
                        }
                    )
                    TelemetryMetricItem(
                        label = "RADIUS",
                        value = String.format(Locale.US, "%.1f R⊕", radius)
                    )
                }
            }

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Track / Lock Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFollowing) Color(0xFF0284C7) else Color(0x330284C7)
                        )
                        .border(
                            1.dp,
                            if (isFollowing) Color(0xFF38BDF8) else Color(0x4438BDF8),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleFollow() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFollowing) "🎯 TRACKING" else "🔭 TRACK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isFollowing) Color.White else Color(0xFF38BDF8)
                    )
                }

                // Delete / Eject Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33EF4444))
                        .border(1.dp, Color(0x55EF4444), RoundedCornerShape(8.dp))
                        .clickable { onDeleteBody() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗑️ EJECT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF87171)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFE2E8F0)
        )
    }
}
