package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.model.SystemConfig

val LocalColdCacheColors = staticCompositionLocalOf<ColdCacheColors> {
    error("No ColdCacheColors provided")
}

val LocalColdCacheShapes = staticCompositionLocalOf<ColdCacheShapes> {
    error("No ColdCacheShapes provided")
}

@Composable
fun ColdCacheTheme(
    config: SystemConfig,
    content: @Composable () -> Unit
) {
    val coldCacheColors = getColdCacheColors(config)
    val coldCacheShapes = getColdCacheShapes(config.uiShape)

    val materialColors = if (coldCacheColors.isDark) {
        darkColorScheme(
            background = coldCacheColors.bgBase,
            surface = coldCacheColors.bgPanel,
            primary = coldCacheColors.accent1,
            secondary = coldCacheColors.accent2,
            onBackground = coldCacheColors.textMain,
            onSurface = coldCacheColors.textMain
        )
    } else {
        lightColorScheme(
            background = coldCacheColors.bgBase,
            surface = coldCacheColors.bgPanel,
            primary = coldCacheColors.accent1,
            secondary = coldCacheColors.accent2,
            onBackground = coldCacheColors.textMain,
            onSurface = coldCacheColors.textMain
        )
    }

    CompositionLocalProvider(
        LocalColdCacheColors provides coldCacheColors,
        LocalColdCacheShapes provides coldCacheShapes
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = ColdCacheTypography,
            content = content
        )
    }
}
