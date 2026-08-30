package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UiShapeStyle

val ColdCacheTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.8.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 0.5.sp
    )
)

data class ColdCacheShapes(
    val primary: Shape,
    val secondary: Shape
)

fun getColdCacheShapes(style: UiShapeStyle): ColdCacheShapes {
    return when (style) {
        UiShapeStyle.PETAL -> ColdCacheShapes(
            primary = RoundedCornerShape(topStart = 16.dp, topEnd = 3.dp, bottomEnd = 16.dp, bottomStart = 3.dp),
            secondary = RoundedCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
        )
        UiShapeStyle.DIAG -> ColdCacheShapes(
            primary = CutCornerShape(topStart = 10.dp, topEnd = 0.dp, bottomEnd = 10.dp, bottomStart = 0.dp),
            secondary = CutCornerShape(topStart = 5.dp, topEnd = 0.dp, bottomEnd = 5.dp, bottomStart = 0.dp)
        )
        UiShapeStyle.SHARP -> ColdCacheShapes(
            primary = RectangleShape,
            secondary = RectangleShape
        )
        UiShapeStyle.SOFT -> ColdCacheShapes(
            primary = RoundedCornerShape(12.dp),
            secondary = RoundedCornerShape(6.dp)
        )
    }
}
