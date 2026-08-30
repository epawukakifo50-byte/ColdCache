package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val state: String, // BUFFER, ACTIVE_RAM, CRYO, ARCHIVED
    val weight: Int,
    val progress: Int,
    val subtasksJson: String,
    val scheduledDate: String?,
    val scheduledTime: String?,
    val completedAt: String?,
    val createdAt: Long
)
