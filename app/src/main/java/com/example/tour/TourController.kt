package com.example.tour

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

class TourController(
    private val onTourFinished: () -> Unit = {}
) {
    var isTourActive by mutableStateOf(false)
        private set

    var currentScenario by mutableStateOf<TourScenario?>(null)
        private set

    var currentStepIndex by mutableStateOf(0)
        private set

    val targetBounds = mutableStateMapOf<TourTargetId, Rect>()

    val currentStep: TourStep?
        get() {
            val scenario = currentScenario ?: return null
            if (currentStepIndex in scenario.steps.indices) {
                return scenario.steps[currentStepIndex]
            }
            return null
        }

    val currentTargetRect: Rect?
        get() {
            val step = currentStep ?: return null
            return targetBounds[step.targetId]
        }

    val totalSteps: Int
        get() = currentScenario?.steps?.size ?: 0

    fun registerTarget(targetId: TourTargetId, bounds: Rect) {
        targetBounds[targetId] = bounds
    }

    fun startTour(scenario: TourScenario = TourScenarios.DASHBOARD_CORE) {
        currentScenario = scenario
        currentStepIndex = 0
        isTourActive = true
    }

    fun nextStep() {
        val scenario = currentScenario ?: return
        if (currentStepIndex < scenario.steps.size - 1) {
            currentStepIndex += 1
        } else {
            dismissTour()
        }
    }

    fun previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex -= 1
        }
    }

    fun dismissTour() {
        isTourActive = false
        currentScenario = null
        currentStepIndex = 0
        onTourFinished()
    }
}

val LocalTourController = staticCompositionLocalOf<TourController?> { null }

fun Modifier.tourTarget(
    targetId: TourTargetId,
    tourController: TourController? = null
): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        tourController?.registerTarget(targetId, bounds)
    }
)
