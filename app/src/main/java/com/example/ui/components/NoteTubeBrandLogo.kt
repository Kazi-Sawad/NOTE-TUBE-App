package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * NoteTubeBrandLogo: High-fidelity, detailed representation of the 3D brand logo.
 * Features:
 * - Ambient warm red glow aura behind the token
 * - Polished rounded card container with dark obsidian brushed background
 * - Specular border highlight giving tangible glass depth
 * - Centered high-detail vector asset (folded document, note lines, play button, dynamic 3D red ribbon wrap)
 * - Optional subtle breathing glow for eye-catching hero moments
 */
@Composable
fun NoteTubeBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showContainerBackground: Boolean = true,
    animatedGlow: Boolean = false
) {
    val cornerRadius = size * 0.28f

    if (showContainerBackground) {
        val glowAlpha = if (animatedGlow) {
            val infiniteTransition = rememberInfiniteTransition(label = "LogoGlow")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "LogoGlowAlpha"
            )
            alpha
        } else {
            0.45f
        }

        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Ambient red backing aura
            Box(
                modifier = Modifier
                    .size(size * 1.15f)
                    .clip(RoundedCornerShape(cornerRadius * 1.2f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE50914).copy(alpha = glowAlpha * 0.45f),
                                Color(0xFFFF2644).copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Main obsidian token
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = (size.value * 0.15f).dp.coerceAtLeast(3.dp),
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = Color(0x70E50914),
                        ambientColor = Color(0x60000000)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E212A),
                                Color(0xFF13151B),
                                Color(0xFF0C0D11)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color(0xFFE50914).copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_notetube_logo),
                    contentDescription = "NoteTube Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.06f)
                )
            }
        }
    } else {
        // Direct mark without container
        Image(
            painter = painterResource(id = R.drawable.ic_notetube_logo),
            contentDescription = "NoteTube Logo",
            modifier = modifier.size(size)
        )
    }
}
