package com.example.model

enum class ColorMode {
    DARK,
    LIGHT
}

enum class Terminology {
    SYSTEM,
    HUMAN
}

enum class UiShapeStyle {
    PETAL, // Diagonal rounded / Leaf petal shape
    DIAG,  // Tech - Diagonal cut
    SHARP, // Strict - Rectangular
    SOFT   // Bio - Uniform rounded
}

enum class ColorStyle {
    FLAT,
    GRADIENT
}

enum class AppSystemState {
    NORMAL,
    COMPILING,
    SAFE_MODE
}

enum class SensoryTheme(val displayName: String, val desc: String, val primaryHex: String, val secondaryHex: String) {
    ORIGINAL_STEEL("ORIGINAL GREY (DEFAULT)", "Оригинальная серая: Полностью нейтральный графит и лаймовый неон", "#a3e635", "#64748b"),
    CYBER_NEON("CYBER MATRIX", "Киберпанк: Лазерный циан & Электрическая фуксия", "#00f0ff", "#ff007f"),
    AMBER_PHOSPHOR("AMBER PHOSPHOR", "Теплый янтарный CRT (0% синего света, мягко для глаз)", "#ffb300", "#ff6f00"),
    MUTED_SAGE("BIO FLORA", "Успокаивающий био-мох & изумрудное свечение", "#10b981", "#6ee7b7"),
    VAPOR_SYNTH("VAPOR SYNTH", "Ретровейв 80-х: Неоновый закат & Ультрафиолет", "#f43f5e", "#a855f7"),
    GLACIER_FROST("NORDIC FROST", "Арктический лед: Морозный аквамарин & Чистый снег", "#38bdf8", "#e0f2fe"),
    SOLAR_FLARE("SOLAR FLARE", "Солнечная энергия: Неоновый мандарин & Золотой песок", "#f97316", "#fbbf24"),
    LAVENDER_AMETHYST("LAVENDER VELVET", "Мягкий анти-стресс: Нежная лаванда & Аметист", "#c084fc", "#e879f9"),
    MONOKAI_PRO("MONOKAI TOKYO", "Хакерский стиль: Радиоактивный лайм & Сочный апельсин", "#a6e22e", "#fd971f")
}

data class SystemConfig(
    val colorMode: ColorMode = ColorMode.DARK,
    val sensoryTheme: SensoryTheme = SensoryTheme.ORIGINAL_STEEL,
    val terminology: Terminology = Terminology.SYSTEM,
    val uiShape: UiShapeStyle = UiShapeStyle.DIAG,
    val colorStyle: ColorStyle = ColorStyle.FLAT,
    val accent1: String = "#a3e635",
    val accent2: String = "#64748b",
    val glowLevel: Int = 20,
    val daemonShadeTracker: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val ramIdleReminderEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val thermalDissipationEnabled: Boolean = true,
    val quickBufferInShade: Boolean = true
)
