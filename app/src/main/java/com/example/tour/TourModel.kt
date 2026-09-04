package com.example.tour

import androidx.compose.ui.geometry.Rect

enum class TourTargetId {
    SYSTEM_HEADER,
    MODULAR_DAEMONS,
    BUFFER_BUTTON,
    ACTIVE_RAM,
    CRYO_STORAGE,
    DEV_NULL,
    SETTINGS_BUTTON,
    FLOW_BATTERY,
    SUBTASKS_SECTION,
    FINISH_BUTTON
}

enum class TourPlacement {
    TOP,
    BOTTOM,
    AUTO
}

enum class TourActionType {
    NONE,
    CLICK,
    TAP_TARGET
}

data class TourStep(
    val id: String,
    val targetId: TourTargetId,
    val title: String,
    val description: String,
    val actionHint: String? = null,
    val placement: TourPlacement = TourPlacement.AUTO,
    val spotlightPaddingDp: Float = 8f,
    val spotlightCornerRadiusDp: Float = 12f,
    val actionRequired: TourActionType = TourActionType.NONE
)

data class TourScenario(
    val id: String,
    val title: String,
    val steps: List<TourStep>
)
