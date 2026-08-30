package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.Subtask
import com.example.model.Task
import com.example.model.TaskState
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromSubtasksList(subtasks: List<Subtask>): String {
        val array = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("done", item.done)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubtasksList(jsonString: String?): List<Subtask> {
        if (jsonString.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<Subtask>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.optString("id", "s-$i"),
                        text = obj.optString("text", ""),
                        done = obj.optBoolean("done", false)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        state = try { TaskState.valueOf(state) } catch (_: Exception) { TaskState.BUFFER },
        weight = weight,
        progress = progress,
        subtasks = Converters().toSubtasksList(subtasksJson),
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        completedAt = completedAt,
        createdAt = createdAt
    )
}

fun Task.toEntity(overrideState: String? = null): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        state = overrideState ?: state.name,
        weight = weight,
        progress = progress,
        subtasksJson = Converters().fromSubtasksList(subtasks),
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        completedAt = completedAt,
        createdAt = createdAt
    )
}
