package com.whitestore.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.whitestore.data.prefs.BadgeShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive shapes for badges and UI accents.
 */
object M3Shapes {

    /**
     * Cookie / Scalloped biscuit shape with 12 soft round lobes (Material 3 Expressive "cookie").
     */
    val CookieShape: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rOuter = minOf(cx, cy)
        val rInner = rOuter * 0.84f
        val lobes = 12
        val step = (2 * PI) / lobes

        for (i in 0 until lobes) {
            val angle = i * step
            val nextAngle = (i + 1) * step
            val midAngle = angle + (step / 2f)

            val xStart = cx + (rInner * cos(angle)).toFloat()
            val yStart = cy + (rInner * sin(angle)).toFloat()

            val xCtrl = cx + (rOuter * 1.08f * cos(midAngle)).toFloat()
            val yCtrl = cy + (rOuter * 1.08f * sin(midAngle)).toFloat()

            val xEnd = cx + (rInner * cos(nextAngle)).toFloat()
            val yEnd = cy + (rInner * sin(nextAngle)).toFloat()

            if (i == 0) {
                moveTo(xStart, yStart)
            }
            quadraticTo(xCtrl, yCtrl, xEnd, yEnd)
        }
        close()
    }

    /**
     * Diamond / Rhombus shape with slightly rounded tips.
     */
    val DiamondShape: Shape = GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        moveTo(w / 2f, 0f)
        lineTo(w, h / 2f)
        lineTo(w / 2f, h)
        lineTo(0f, h / 2f)
        close()
    }

    /**
     * 8-pointed Material 3 Star shape.
     */
    val StarShape: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rOuter = minOf(cx, cy)
        val rInner = rOuter * 0.72f
        val points = 8
        val step = PI / points

        for (i in 0 until (points * 2)) {
            val r = if (i % 2 == 0) rOuter else rInner
            val angle = i * step - (PI / 2)
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }

    /**
     * Slanted / Asymmetrical pill cut shape: top-left & bottom-right large radius, others small.
     */
    val SlantedShape: Shape = RoundedCornerShape(topStart = 11.dp, topEnd = 3.dp, bottomEnd = 11.dp, bottomStart = 3.dp)

    /**
     * Squircle (smooth rounded square).
     */
    val SquircleShape: Shape = RoundedCornerShape(7.dp)

    /**
     * Returns the Compose Shape corresponding to the user preference [BadgeShape].
     */
    fun getBadgeShape(badgeShape: BadgeShape): Shape {
        return when (badgeShape) {
            BadgeShape.CIRCLE -> CircleShape
            BadgeShape.COOKIE -> CookieShape
            BadgeShape.SQUIRCLE -> SquircleShape
            BadgeShape.SLANTED -> SlantedShape
            BadgeShape.DIAMOND -> DiamondShape
            BadgeShape.STAR -> StarShape
        }
    }
}
