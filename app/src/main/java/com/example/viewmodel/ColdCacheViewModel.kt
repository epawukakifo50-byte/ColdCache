package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferenceManager
import com.example.data.repository.TaskRepository
import com.example.model.*
import com.example.service.DaemonTrackerService
import com.example.service.TaskScheduler
import com.example.widget.ColdCacheWidgetProvider
import com.example.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderedDaemonMap(
    val map: Map<String, Daemon>,
    val version: Long = System.nanoTime()
) : Map<String, Daemon> by map {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedDaemonMap) return false
        return version == other.version && map.keys.toList() == other.map.keys.toList()
    }
    override fun hashCode(): Int = version.hashCode()
}

class ColdCacheViewModel(
    private val repository: TaskRepository,
    private val prefManager: PreferenceManager,
    private val appContext: Context
) : ViewModel() {

    // --- System Config & Daemons ---
    private val _systemConfig = MutableStateFlow(prefManager.loadSystemConfig())
    val systemConfig: StateFlow<SystemConfig> = _systemConfig.asStateFlow()

    private val _daemons = MutableStateFlow<Map<String, Daemon>>(OrderedDaemonMap(prefManager.loadDaemons()))
    val daemons: StateFlow<Map<String, Daemon>> = _daemons.asStateFlow()

    private val _systemState = MutableStateFlow(prefManager.loadSystemState())
    val systemState: StateFlow<AppSystemState> = _systemState.asStateFlow()

    // --- Tasks & Archive ---
    val allActiveTasks: StateFlow<List<Task>> = repository.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val renderLog: StateFlow<List<Task>> = repository.archivedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRamTasks: StateFlow<List<Task>> = allActiveTasks.map { list ->
        list.filter { it.state == TaskState.ACTIVE_RAM }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cryoTasks: StateFlow<List<Task>> = allActiveTasks.map { list ->
        list.filter { it.state == TaskState.CRYO }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bufferTasks: StateFlow<List<Task>> = allActiveTasks.map { list ->
        list.filter { it.state == TaskState.BUFFER }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Compiling (Hyperfocus) State ---
    private val _activeColliderTask = MutableStateFlow<Task?>(null)
    val activeColliderTask: StateFlow<Task?> = _activeColliderTask.asStateFlow()

    // --- Modal States ---
    private val _isBufferOpen = MutableStateFlow(false)
    val isBufferOpen: StateFlow<Boolean> = _isBufferOpen.asStateFlow()

    private val _isTemporalOpen = MutableStateFlow(false)
    val isTemporalOpen: StateFlow<Boolean> = _isTemporalOpen.asStateFlow()

    private val _isLogOpen = MutableStateFlow(false)
    val isLogOpen: StateFlow<Boolean> = _isLogOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isMatrixOpen = MutableStateFlow(false)
    val isMatrixOpen: StateFlow<Boolean> = _isMatrixOpen.asStateFlow()

    private val _isManualOpen = MutableStateFlow(false)
    val isManualOpen: StateFlow<Boolean> = _isManualOpen.asStateFlow()

    private val _selectedHeatmapDaemon = MutableStateFlow<Daemon?>(null)
    val selectedHeatmapDaemon: StateFlow<Daemon?> = _selectedHeatmapDaemon.asStateFlow()

    fun openDaemonHeatmap(daemon: Daemon?) {
        _selectedHeatmapDaemon.value = daemon
    }

    private val _schedulingTask = MutableStateFlow<Task?>(null)
    val schedulingTask: StateFlow<Task?> = _schedulingTask.asStateFlow()

    private val _ramOverflowTask = MutableStateFlow<Task?>(null)
    val ramOverflowTask: StateFlow<Task?> = _ramOverflowTask.asStateFlow()

    // --- Recurring Schedule Slots (Academic / Classes / Timetable) ---
    private val _scheduleSlots = MutableStateFlow<List<ScheduleSlot>>(prefManager.loadScheduleSlots())
    val scheduleSlots: StateFlow<List<ScheduleSlot>> = _scheduleSlots.asStateFlow()

    fun addScheduleSlot(slot: ScheduleSlot) {
        val updated = _scheduleSlots.value + slot
        _scheduleSlots.value = updated
        prefManager.saveScheduleSlots(updated)
    }

    fun updateScheduleSlot(slot: ScheduleSlot) {
        val updated = _scheduleSlots.value.map { if (it.id == slot.id) slot else it }
        _scheduleSlots.value = updated
        prefManager.saveScheduleSlots(updated)
    }

    fun deleteScheduleSlot(slotId: String) {
        val updated = _scheduleSlots.value.filter { it.id != slotId }
        _scheduleSlots.value = updated
        prefManager.saveScheduleSlots(updated)
    }

    // --- In-place Editing & DevNull ---
    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()
    val editingTaskId: StateFlow<String?> = _editingTask.map { it?.id }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isBufferReversed = MutableStateFlow(true)
    val isBufferReversed: StateFlow<Boolean> = _isBufferReversed.asStateFlow()

    // --- Thermal Dissipation State ---
    private val _completedDissipationTask = MutableStateFlow<Task?>(null)
    val completedDissipationTask: StateFlow<Task?> = _completedDissipationTask.asStateFlow()

    fun dismissThermalDissipation() {
        _completedDissipationTask.value = null
    }

    // --- In-App Updates ---
    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    fun checkForUpdates(isManual: Boolean = false, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = UpdateChecker.checkForUpdate()
            _isCheckingUpdate.value = false
            if (info != null && info.isUpdateAvailable) {
                _availableUpdate.value = info
                onResult?.invoke(true)
            } else {
                if (isManual) {
                    _availableUpdate.value = null
                }
                onResult?.invoke(false)
            }
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "cc_daemons", "cc_daemons_array", "cc_date" -> {
                _daemons.value = OrderedDaemonMap(prefManager.loadDaemons(), System.nanoTime())
            }
            "cc_mode", "cc_sensory_theme", "cc_term", "cc_shape", "cc_color_style",
            "cc_accent1", "cc_accent2", "cc_glow", "cc_daemon_shade", "cc_task_reminders",
            "cc_ram_idle", "cc_haptic", "cc_thermal_fx", "cc_quick_buffer_shade" -> {
                _systemConfig.value = prefManager.loadSystemConfig()
            }
            "cc_state" -> {
                _systemState.value = prefManager.loadSystemState()
            }
        }
    }

    init {
        prefManager.registerChangeListener(prefChangeListener)

        // Restore active collider task if in COMPILING state
        viewModelScope.launch {
            val savedId = prefManager.loadActiveTaskId()
            if (savedId != null && _systemState.value == AppSystemState.COMPILING) {
                val list = repository.getAllTasksSnapshot()
                val found = list.find { it.id == savedId }
                _activeColliderTask.value = found
                if (found == null) {
                    _systemState.value = AppSystemState.NORMAL
                    prefManager.saveSystemState(AppSystemState.NORMAL)
                }
            }
        }

        // Auto sync widgets and shade tracker when active RAM tasks change
        viewModelScope.launch {
            activeRamTasks.collect { ramList ->
                ColdCacheWidgetProvider.updateAllWidgets(appContext)

                // Check RAM Idle logic: if 0 tasks in RAM and enabled, start 1-hour alarm
                if (_systemConfig.value.ramIdleReminderEnabled) {
                    if (ramList.isEmpty()) {
                        TaskScheduler.scheduleRamIdleAlarm(appContext)
                    } else {
                        TaskScheduler.cancelRamIdleAlarm(appContext)
                    }
                }
            }
        }

        // Automatic weekly encrypted backup check
        viewModelScope.launch(Dispatchers.IO) {
            delay(3000)
            com.example.util.EncryptedBackupManager.checkAndPerformWeeklyBackup(appContext) {
                exportMemoryDumpJson()
            }
        }

        // Automatic first-run interactive tour check
        viewModelScope.launch {
            delay(600)
            if (!prefManager.hasCompletedTour() && _systemState.value == AppSystemState.NORMAL) {
                tourController.startTour(com.example.tour.TourScenarios.DASHBOARD_CORE)
            }
        }

        // Background app update check from GitHub
        viewModelScope.launch {
            delay(4000)
            checkForUpdates(isManual = false)
        }
    }

    val tourController = com.example.tour.TourController(
        onTourFinished = {
            prefManager.setTourCompleted(true)
        }
    )

    fun startInteractiveTour() {
        prefManager.setTourCompleted(false)
        tourController.startTour(com.example.tour.TourScenarios.DASHBOARD_CORE)
    }

    fun refreshFromExternalSources() {
        _daemons.value = prefManager.loadDaemons()
        _systemConfig.value = prefManager.loadSystemConfig()
        _systemState.value = prefManager.loadSystemState()
    }

    override fun onCleared() {
        super.onCleared()
        prefManager.unregisterChangeListener(prefChangeListener)
    }

    private fun syncExternalViews() {
        ColdCacheWidgetProvider.updateAllWidgets(appContext)
        if (_systemConfig.value.daemonShadeTracker) {
            DaemonTrackerService.updateNotification(appContext, _daemons.value)
            com.example.service.NotificationHelper.showOrUpdateDaemonNotification(appContext)
        }
    }

    // --- System Config Actions ---
    fun updateSystemConfig(transform: (SystemConfig) -> SystemConfig) {
        val prev = _systemConfig.value
        val updated = transform(prev)
        _systemConfig.value = updated
        prefManager.saveSystemConfig(updated)

        if (prev.daemonShadeTracker != updated.daemonShadeTracker) {
            if (updated.daemonShadeTracker) {
                DaemonTrackerService.start(appContext)
            } else {
                DaemonTrackerService.stop(appContext)
            }
        }

        if (prev.ramIdleReminderEnabled != updated.ramIdleReminderEnabled) {
            if (updated.ramIdleReminderEnabled && activeRamTasks.value.isEmpty()) {
                TaskScheduler.scheduleRamIdleAlarm(appContext)
            } else {
                TaskScheduler.cancelRamIdleAlarm(appContext)
            }
        }
    }

    fun setSystemState(state: AppSystemState) {
        _systemState.value = state
        prefManager.saveSystemState(state)
    }

    // --- Daemon Actions ---
    fun interactDaemon(key: String) {
        val currentDaemons = _daemons.value.toMutableMap()
        val d = currentDaemons[key] ?: return
        val newCurrent = d.current + d.step
        currentDaemons[key] = d.copy(current = newCurrent)
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)

        if (d.type == DaemonType.SENSOR_STEPS) {
            stepSensorManager.resetBaseline(newCurrent)
        }

        syncExternalViews()
        com.example.util.AppHaptics.tick(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun resetDaemon(key: String) {
        val currentDaemons = _daemons.value.toMutableMap()
        val d = currentDaemons[key] ?: return
        currentDaemons[key] = d.copy(current = 0)
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)

        if (d.type == DaemonType.SENSOR_STEPS) {
            stepSensorManager.resetBaseline(0)
        }

        syncExternalViews()
        com.example.util.AppHaptics.click(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun adjustDaemon(key: String, delta: Int) {
        val currentDaemons = _daemons.value.toMutableMap()
        val d = currentDaemons[key] ?: return
        val newCurrent = (d.current + delta).coerceAtLeast(0)
        currentDaemons[key] = d.copy(current = newCurrent)
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)

        if (d.type == DaemonType.SENSOR_STEPS) {
            stepSensorManager.resetBaseline(newCurrent)
        }

        syncExternalViews()
        com.example.util.AppHaptics.tick(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun setDaemonDayProgress(key: String, dateStr: String, current: Int, max: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefManager.recordDaemonProgressForDate(key, dateStr, current, max)
        if (dateStr == today) {
            val currentDaemons = _daemons.value.toMutableMap()
            val d = currentDaemons[key]
            if (d != null) {
                currentDaemons[key] = d.copy(current = current)
                _daemons.value = OrderedDaemonMap(currentDaemons)
                prefManager.saveDaemons(currentDaemons)
                if (d.type == DaemonType.SENSOR_STEPS) {
                    stepSensorManager.resetBaseline(current)
                }
            }
        }
        syncExternalViews()
    }

    private val stepSensorManager = com.example.sensor.StepSensorManager.getInstance(appContext).apply {
        onStepsUpdated = {
            _daemons.value = OrderedDaemonMap(prefManager.loadDaemons())
            syncExternalViews()
        }
        startListening()
    }

    fun checkDailyRollover() {
        val didReset = prefManager.checkAndPerformDailyRollover()
        if (didReset) {
            _daemons.value = OrderedDaemonMap(prefManager.loadDaemons())
            stepSensorManager.resetBaseline(0)
            syncExternalViews()
        }
    }

    fun restartStepSensor() {
        stepSensorManager.restartListening()
    }

    fun addCustomDaemon(label: String, max: Int, step: Int, iconName: String, type: DaemonType = DaemonType.MANUAL, colorHex: String? = null, overColorHex: String? = null) {
        val currentDaemons = _daemons.value.toMutableMap()
        val newKey = "d_${System.currentTimeMillis()}"
        val palette = listOf("#acf002", "#06b6d4", "#a855f7", "#ec4899", "#f59e0b", "#3b82f6", "#10b981")
        val assignedColor = colorHex ?: if (type == DaemonType.SENSOR_STEPS) "#acf002" else palette[currentDaemons.size % palette.size]
        val assignedOverColor = overColorHex ?: when (assignedColor) {
            "#acf002" -> "#ec4899"
            "#06b6d4" -> "#a855f7"
            "#f00281" -> "#eab308"
            else -> "#ec4899"
        }
        val newDaemon = Daemon(
            key = newKey,
            label = label.trim().ifBlank { "DAEMON" },
            current = 0,
            max = max.coerceAtLeast(1),
            step = step.coerceAtLeast(1),
            iconName = iconName,
            type = type,
            colorHex = assignedColor,
            overColorHex = assignedOverColor
        )
        currentDaemons[newKey] = newDaemon
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)
        syncExternalViews()
        com.example.util.AppHaptics.success(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun deleteCustomDaemon(key: String) {
        val currentDaemons = _daemons.value.toMutableMap()
        currentDaemons.remove(key)
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)
        syncExternalViews()
        com.example.util.AppHaptics.snap(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun updateDaemonFull(daemon: Daemon) {
        val currentDaemons = _daemons.value.toMutableMap()
        currentDaemons[daemon.key] = daemon
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)
        if (_selectedHeatmapDaemon.value?.key == daemon.key) {
            _selectedHeatmapDaemon.value = daemon
        }
        syncExternalViews()
    }

    fun moveDaemon(key: String, direction: Int) {
        val list = _daemons.value.values.toMutableList()
        val index = list.indexOfFirst { it.key == key }
        if (index < 0) return
        val targetIndex = index + direction
        if (targetIndex < 0 || targetIndex >= list.size) return
        val item = list.removeAt(index)
        list.add(targetIndex, item)
        val orderedMap = linkedMapOf<String, Daemon>()
        list.forEach { orderedMap[it.key] = it }
        _daemons.value = OrderedDaemonMap(orderedMap)
        prefManager.saveDaemons(orderedMap)
        syncExternalViews()
        com.example.util.AppHaptics.tick(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun updateDaemonConfig(key: String, label: String? = null, max: Int? = null, step: Int? = null, iconName: String? = null, type: DaemonType? = null, colorHex: String? = null, overColorHex: String? = null) {
        val currentDaemons = _daemons.value.toMutableMap()
        val d = currentDaemons[key] ?: return
        currentDaemons[key] = d.copy(
            label = label ?: d.label,
            max = max ?: d.max,
            step = step ?: d.step,
            iconName = iconName ?: d.iconName,
            type = type ?: d.type,
            colorHex = colorHex ?: d.colorHex,
            overColorHex = overColorHex ?: d.overColorHex
        )
        _daemons.value = OrderedDaemonMap(currentDaemons)
        prefManager.saveDaemons(currentDaemons)
        if (_selectedHeatmapDaemon.value?.key == key) {
            _selectedHeatmapDaemon.value = currentDaemons[key]
        }
        syncExternalViews()
    }

    // --- Task CRUD ---
    fun createNewTask(title: String) {
        if (title.isBlank()) return
        val newTask = Task(
            title = title.trim(),
            state = TaskState.BUFFER
        )
        viewModelScope.launch {
            repository.insertTask(newTask)
        }
    }

    fun startEditTask(task: Task) {
        _editingTask.value = task
    }

    fun closeEditTask() {
        _editingTask.value = null
    }

    fun saveTaskDetails(updatedTask: Task) {
        viewModelScope.launch {
            repository.updateTask(updatedTask)
            if (_activeColliderTask.value?.id == updatedTask.id) {
                _activeColliderTask.value = updatedTask
            }
            if (_systemConfig.value.taskRemindersEnabled) {
                if (updatedTask.scheduledDate.isNullOrBlank()) {
                    TaskScheduler.cancelTaskReminders(appContext, updatedTask.id)
                } else {
                    TaskScheduler.scheduleTaskReminders(appContext, updatedTask)
                }
            }
            com.example.util.AppHaptics.snap(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
        _editingTask.value = null
    }

    fun saveEditTask(taskId: String, newTitle: String) {
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                val current = allActiveTasks.value.find { it.id == taskId }
                if (current != null) {
                    val updated = current.copy(title = newTitle.trim())
                    repository.updateTask(updated)
                    if (_activeColliderTask.value?.id == taskId) {
                        _activeColliderTask.value = updated
                    }
                }
            }
        }
        _editingTask.value = null
    }

    fun cancelEditTask() {
        _editingTask.value = null
    }

    fun moveTask(taskId: String, targetState: TaskState) {
        viewModelScope.launch {
            val task = allActiveTasks.value.find { it.id == taskId } ?: return@launch
            if (targetState == TaskState.ACTIVE_RAM && task.state != TaskState.ACTIVE_RAM) {
                val currentRamCount = activeRamTasks.value.size
                if (currentRamCount >= 2) {
                    _ramOverflowTask.value = task
                    com.example.util.AppHaptics.overloadWarning(appContext, _systemConfig.value.hapticFeedbackEnabled)
                    return@launch
                }
            }
            repository.moveTask(task, targetState)
            com.example.util.AppHaptics.snap(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
    }

    fun replaceRamTask(ramTaskToCryo: Task, incomingTask: Task) {
        viewModelScope.launch {
            repository.moveTask(ramTaskToCryo, TaskState.CRYO)
            repository.moveTask(incomingTask, TaskState.ACTIVE_RAM)
            _ramOverflowTask.value = null
            com.example.util.AppHaptics.snap(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
    }

    fun performManualEncryptedBackup(): java.io.File? {
        val dump = exportMemoryDumpJson()
        val file = com.example.util.EncryptedBackupManager.createEncryptedBackup(appContext, dump)
        if (file != null) {
            com.example.util.AppHaptics.success(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
        return file
    }

    fun restoreEncryptedBackup(file: java.io.File): Boolean {
        val json = com.example.util.EncryptedBackupManager.decryptBackupFile(file) ?: return false
        val success = importMemoryDumpJson(json)
        if (success) {
            com.example.util.AppHaptics.success(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
        return success
    }

    fun dropTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            TaskScheduler.cancelTaskReminders(appContext, taskId)
            com.example.util.AppHaptics.dumpRelease(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
    }

    fun restoreFromArchive(taskId: String) {
        viewModelScope.launch {
            val task = renderLog.value.find { it.id == taskId } ?: return@launch
            repository.restoreArchivedTask(task)
            com.example.util.AppHaptics.snap(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
    }

    fun updateTaskSchedule(taskId: String, date: String?, time: String?) {
        viewModelScope.launch {
            val task = allActiveTasks.value.find { it.id == taskId } ?: return@launch
            val updated = task.copy(scheduledDate = date, scheduledTime = time)
            repository.updateTask(updated)
            if (_activeColliderTask.value?.id == taskId) {
                _activeColliderTask.value = updated
            }
            if (_systemConfig.value.taskRemindersEnabled) {
                if (date.isNullOrBlank()) {
                    TaskScheduler.cancelTaskReminders(appContext, taskId)
                } else {
                    TaskScheduler.scheduleTaskReminders(appContext, updated)
                }
            }
            com.example.util.AppHaptics.click(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
    }

    // --- Hyperfocus / Compilation Mode ---
    fun startCompilation(task: Task) {
        _activeColliderTask.value = task
        _systemState.value = AppSystemState.COMPILING
        prefManager.saveSystemState(AppSystemState.COMPILING)
        prefManager.saveActiveTaskId(task.id)
        com.example.util.AppHaptics.click(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    // Task Focus Elapsed Seconds (Persisted in SharedPreferences so quanta never reset)
    fun getTaskFocusSeconds(taskId: String): Int {
        return prefManager.loadTaskFocusSeconds(taskId)
    }

    fun saveTaskFocusSeconds(taskId: String, seconds: Int) {
        prefManager.saveTaskFocusSeconds(taskId, seconds)
    }

    fun clearTaskFocusSeconds(taskId: String) {
        prefManager.clearTaskFocusSeconds(taskId)
    }

    fun exitCompilation(targetState: TaskState) {
        val task = _activeColliderTask.value
        if (task != null) {
            viewModelScope.launch {
                repository.moveTask(task, targetState)
            }
        }
        _activeColliderTask.value = null
        _systemState.value = AppSystemState.NORMAL
        prefManager.saveSystemState(AppSystemState.NORMAL)
        prefManager.saveActiveTaskId(null)
        com.example.util.AppHaptics.click(appContext, _systemConfig.value.hapticFeedbackEnabled)
    }

    fun finishCompilation() {
        val task = _activeColliderTask.value
        if (task != null) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val completed = task.copy(
                completedAt = timestamp,
                progress = 100
            )
            prefManager.clearTaskFocusSeconds(task.id)
            viewModelScope.launch {
                repository.archiveTask(completed)
                TaskScheduler.cancelTaskReminders(appContext, task.id)
            }
            com.example.util.AppHaptics.success(appContext, _systemConfig.value.hapticFeedbackEnabled)
        }
        _activeColliderTask.value = null
        _systemState.value = AppSystemState.NORMAL
        prefManager.saveSystemState(AppSystemState.NORMAL)
        prefManager.saveActiveTaskId(null)
    }

    fun updateProgress(amount: Int) {
        val current = _activeColliderTask.value ?: return
        val newProgress = (current.progress + amount).coerceIn(0, 100)
        val updated = current.copy(progress = newProgress)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    fun toggleSubtask(subId: String) {
        val current = _activeColliderTask.value ?: return
        val updatedSubtasks = current.subtasks.map {
            if (it.id == subId) it.copy(done = !it.done) else it
        }
        val doneCount = updatedSubtasks.count { it.done }
        val newProgress = if (updatedSubtasks.isNotEmpty()) {
            Math.round((doneCount.toFloat() / updatedSubtasks.size) * 100)
        } else current.progress

        val updated = current.copy(subtasks = updatedSubtasks, progress = newProgress)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    fun addSubtask(text: String) {
        if (text.isBlank()) return
        val current = _activeColliderTask.value ?: return
        val newSubtask = Subtask(text = text.trim())
        val updatedSubtasks = current.subtasks + newSubtask
        val doneCount = updatedSubtasks.count { it.done }
        val newProgress = Math.round((doneCount.toFloat() / updatedSubtasks.size) * 100)
        val updated = current.copy(subtasks = updatedSubtasks, progress = newProgress)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    fun deleteSubtask(subId: String) {
        val current = _activeColliderTask.value ?: return
        val updatedSubtasks = current.subtasks.filter { it.id != subId }
        val doneCount = updatedSubtasks.count { it.done }
        val newProgress = if (updatedSubtasks.isNotEmpty()) {
            Math.round((doneCount.toFloat() / updatedSubtasks.size) * 100)
        } else current.progress
        val updated = current.copy(subtasks = updatedSubtasks, progress = newProgress)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    fun updateSubtask(subId: String, newText: String) {
        if (newText.isBlank()) return
        val current = _activeColliderTask.value ?: return
        val updatedSubtasks = current.subtasks.map {
            if (it.id == subId) it.copy(text = newText.trim()) else it
        }
        val updated = current.copy(subtasks = updatedSubtasks)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    fun reorderSubtasks(fromIndex: Int, toIndex: Int) {
        val current = _activeColliderTask.value ?: return
        if (fromIndex !in current.subtasks.indices || toIndex !in current.subtasks.indices) return
        val list = current.subtasks.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val updated = current.copy(subtasks = list)
        _activeColliderTask.value = updated
        viewModelScope.launch {
            repository.updateTask(updated)
        }
    }

    // --- Modal Navigation ---
    fun openBuffer(open: Boolean) { _isBufferOpen.value = open }
    fun openTemporal(open: Boolean) { _isTemporalOpen.value = open }
    fun openLog(open: Boolean) { _isLogOpen.value = open }
    fun openMatrix(open: Boolean) { _isMatrixOpen.value = open }
    fun openSettings(open: Boolean) { _isSettingsOpen.value = open }
    fun openManual(open: Boolean) { _isManualOpen.value = open }
    fun setSchedulingTask(task: Task?) { _schedulingTask.value = task }
    fun setRamOverflowTask(task: Task?) { _ramOverflowTask.value = task }
    fun toggleBufferReversed() { _isBufferReversed.value = !_isBufferReversed.value }

    fun closeAllModals(): Boolean {
        if (_editingTask.value != null) { _editingTask.value = null; return true }
        if (_selectedHeatmapDaemon.value != null) { _selectedHeatmapDaemon.value = null; return true }
        if (_schedulingTask.value != null) { _schedulingTask.value = null; return true }
        if (_ramOverflowTask.value != null) { _ramOverflowTask.value = null; return true }
        if (_isBufferOpen.value) { _isBufferOpen.value = false; return true }
        if (_isTemporalOpen.value) { _isTemporalOpen.value = false; return true }
        if (_isLogOpen.value) { _isLogOpen.value = false; return true }
        if (_isMatrixOpen.value) { _isMatrixOpen.value = false; return true }
        if (_isSettingsOpen.value) { _isSettingsOpen.value = false; return true }
        if (_isManualOpen.value) { _isManualOpen.value = false; return true }
        return false
    }

    fun exportMarkdownJournal() {
        val tasks = allActiveTasks.value
        val archive = renderLog.value
        val daemonsMap = _daemons.value
        com.example.util.MarkdownExportHelper.exportAndShare(appContext, tasks, archive, daemonsMap)
    }

    // --- Export / Import Memory Dump ---
    fun exportMemoryDumpJson(): String {
        val root = JSONObject()
        val tasksArr = JSONArray()
        allActiveTasks.value.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("state", t.state.name)
            obj.put("weight", t.weight)
            obj.put("progress", t.progress)
            obj.put("scheduledDate", t.scheduledDate)
            obj.put("scheduledTime", t.scheduledTime)
            obj.put("completedAt", t.completedAt)
            obj.put("createdAt", t.createdAt)
            val subs = JSONArray()
            t.subtasks.forEach { s ->
                val sobj = JSONObject()
                sobj.put("id", s.id)
                sobj.put("text", s.text)
                sobj.put("done", s.done)
                subs.put(sobj)
            }
            obj.put("subtasks", subs)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        val logArr = JSONArray()
        renderLog.value.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("weight", t.weight)
            obj.put("completedAt", t.completedAt)
            obj.put("createdAt", t.createdAt)
            logArr.put(obj)
        }
        root.put("renderLog", logArr)
        return root.toString(2)
    }

    fun importMemoryDumpJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val parsedTasks = mutableListOf<Task>()
            if (root.has("tasks")) {
                val array = root.getJSONArray("tasks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val subtasks = mutableListOf<Subtask>()
                    if (obj.has("subtasks")) {
                        val sarr = obj.getJSONArray("subtasks")
                        for (j in 0 until sarr.length()) {
                            val sobj = sarr.getJSONObject(j)
                            subtasks.add(
                                Subtask(
                                    id = sobj.optString("id", "s-$j"),
                                    text = sobj.optString("text", ""),
                                    done = sobj.optBoolean("done", false)
                                )
                            )
                        }
                    }
                    parsedTasks.add(
                        Task(
                            id = obj.optString("id", "n-${(1000..9999).random()}"),
                            title = obj.optString("title", ""),
                            state = try { TaskState.valueOf(obj.optString("state", "BUFFER")) } catch (_: Exception) { TaskState.BUFFER },
                            weight = obj.optInt("weight", 20),
                            progress = obj.optInt("progress", 0),
                            subtasks = subtasks,
                            scheduledDate = if (obj.isNull("scheduledDate")) null else obj.optString("scheduledDate"),
                            scheduledTime = if (obj.isNull("scheduledTime")) null else obj.optString("scheduledTime"),
                            completedAt = if (obj.isNull("completedAt")) null else obj.optString("completedAt"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val parsedArchived = mutableListOf<Task>()
            if (root.has("renderLog")) {
                val array = root.getJSONArray("renderLog")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedArchived.add(
                        Task(
                            id = obj.optString("id", "n-${(1000..9999).random()}"),
                            title = obj.optString("title", ""),
                            state = TaskState.BUFFER,
                            weight = obj.optInt("weight", 20),
                            progress = 100,
                            completedAt = obj.optString("completedAt", "00:00"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            viewModelScope.launch {
                repository.importDump(parsedTasks, parsedArchived)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

class ColdCacheViewModelFactory(
    private val repository: TaskRepository,
    private val prefManager: PreferenceManager,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ColdCacheViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ColdCacheViewModel(repository, prefManager, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
