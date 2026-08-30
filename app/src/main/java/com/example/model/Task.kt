package com.example.model

data class Subtask(
    val id: String = "s-${System.currentTimeMillis()}-${(100..999).random()}",
    val text: String,
    val done: Boolean = false
)

enum class TaskState {
    BUFFER,
    ACTIVE_RAM,
    CRYO
}

data class Task(
    val id: String = "n-${(1000..9999).random()}",
    val title: String,
    val state: TaskState = TaskState.BUFFER,
    val weight: Int = (10..50).random(),
    val progress: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val completedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
