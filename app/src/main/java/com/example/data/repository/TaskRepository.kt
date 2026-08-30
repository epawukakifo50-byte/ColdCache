package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.Task
import com.example.model.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {

    val activeTasks: Flow<List<Task>> = taskDao.getAllActiveTasks().map { list ->
        list.map { it.toDomain() }
    }

    val archivedTasks: Flow<List<Task>> = taskDao.getAllArchivedTasks().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllTasksSnapshot(): List<Task> {
        return taskDao.getAllTasksSnapshot().map { it.toDomain() }
    }

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    suspend fun moveTask(task: Task, newState: TaskState) {
        val updated = task.copy(state = newState)
        taskDao.updateTask(updated.toEntity())
    }

    suspend fun archiveTask(task: Task) {
        val completed = task.copy(
            state = TaskState.BUFFER,
            progress = 100
        )
        taskDao.updateTask(completed.toEntity(overrideState = "ARCHIVED"))
    }

    suspend fun restoreArchivedTask(task: Task) {
        val restored = task.copy(
            state = TaskState.CRYO,
            progress = 0,
            subtasks = emptyList()
        )
        taskDao.updateTask(restored.toEntity(overrideState = "CRYO"))
    }

    suspend fun deleteTask(id: String) {
        taskDao.deleteTask(id)
    }

    suspend fun importDump(tasks: List<Task>, archived: List<Task>) {
        taskDao.clearAll()
        val allEntities = tasks.map { it.toEntity() } + archived.map { it.toEntity(overrideState = "ARCHIVED") }
        taskDao.insertAll(allEntities)
    }
}
