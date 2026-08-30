package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE state != 'ARCHIVED' ORDER BY createdAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE state != 'ARCHIVED' ORDER BY createdAt DESC")
    suspend fun getActiveTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE state = 'ARCHIVED' ORDER BY createdAt DESC")
    fun getAllArchivedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSnapshot(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}
