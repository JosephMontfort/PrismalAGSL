package com.styropyr0.prismal.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalDefaultTintAlpha
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.drawPrismalGlassTint
import com.styropyr0.prismal.effects.PrismalAdaptiveTuning
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.interactive.PrismalPressRipple
import com.styropyr0.prismal.shapes.PrismalCapsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Capsule-shaped glass button with press ripple, scale feedback, and optional tint.
 *
 * @param onClick Click handler.
 * @param backdrop Source sampled through the glass.
 * @param isInteractive When false, disables press animations.
 * @param adaptiveLuminance When true, blur/brightness follow [luminance].
 * @param blurRadius Backdrop blur radius. With adaptive luminance, this is the base
 *   radius scaled by [adaptiveTuning] — it is not ignored.
 * @param height Fixed row height, or `null` to let [modifier] / content drive size.
 * @param tint Optional color overlay blended onto the glass surface.
 * @param tintAlpha Overlay alpha for [tint] (default keeps colored glass see-through).
 * @param surfaceColor Optional solid fill drawn on the glass surface.
 * @param adaptiveTuning Caps / scales when [adaptiveLuminance] is true.
 */
@Composable
fun PrismalGlassButton(
    onClick: () -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f },
    blurRadius: Dp = 8.dp,
    height: Dp? = 48.dp,
    tint: Color = Color.Unspecified,
    tintAlpha: Float = PrismalDefaultTintAlpha,
    surfaceColor: Color = Color.Unspecified,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val pressLiftPx = with(density) { 4.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val interactivePrismalSpecular = remember(animationScope, isInteractive) {
        if (isInteractive) PrismalPressRipple(animationScope = animationScope) else null
    }

    Row(
        modifier
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { PrismalCapsule() },
                effects = {
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = adaptiveLuminance,
                        luminance = luminance(),
                        blurRadiusPx = with(density) { blurRadius.toPx() },
                        refractionHeightPx = with(density) { 12.dp.toPx() },
                        refractionAmountPx = with(density) { 24.dp.toPx() },
                        adaptiveTuning = adaptiveTuning,
                    )
                },
                layerBlock = if (interactivePrismalSpecular != null) {
                    {
                        val width = size.width
                        val heightPx = size.height
                        val progress = interactivePrismalSpecular.pressProgress
                        val scale = lerp(1f, 1f + pressLiftPx / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactivePrismalSpecular.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = pressLiftPx / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / heightPx).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (heightPx / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    drawPrismalGlassTint(tint, tintAlpha)
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (interactivePrismalSpecular != null) {
                    Modifier
                        .then(interactivePrismalSpecular.modifier)
                        .then(interactivePrismalSpecular.gestureModifier)
                } else {
                    Modifier
                }
            )
            .then(if (height != null) Modifier.height(height) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
