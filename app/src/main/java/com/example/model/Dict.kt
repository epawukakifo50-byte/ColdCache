package com.example.model

object Dict {
    private val systemDict = mapOf(
        "ram" to "Active RAM",
        "cryo" to "Cryo-Storage",
        "buffer" to "Latent Space",
        "archive" to "Кристаллизация",
        "safeMode" to "Система в гибернации",
        "render" to "Рендер",
        "compile" to "ИЗОЛЯЦИЯ",
        "drop" to "Drop",
        "inject" to "inject_node",
        "manual" to "ReadMe",
        "settings" to "Системные параметры",
        "emptyMem" to "Память свободна",
        "emptyCryo" to "Отсек пуст",
        "temporal" to "Temporal_Flux",
        "overload" to "SYSTEM_OVERLOAD",
        "overloadMsg" to "Active RAM is full. Cannot move task:",
        "overloadPrompt" to "Would you like to move it to CRYO storage instead?",
        "moveToCryo" to "Move_To_Cryo",
        "cancel" to "Cancel",
        "streamMode" to "Stream",
        "matrixMode" to "Matrix"
    )

    private val humanDict = mapOf(
        "ram" to "В фокусе",
        "cryo" to "Отложено",
        "buffer" to "Входящие мысли",
        "archive" to "Завершено",
        "safeMode" to "Режим отдыха",
        "render" to "Начать",
        "compile" to "В ПРОЦЕССЕ",
        "drop" to "Удалить",
        "inject" to "Записать мысль",
        "manual" to "Как использовать",
        "settings" to "Настройки",
        "emptyMem" to "Пространство свободно",
        "emptyCryo" to "Хранилище свободно",
        "temporal" to "Календарь",
        "overload" to "Слоты внимания заняты (2/2)",
        "overloadMsg" to "Чтобы сохранить фокус без перегрузки:",
        "overloadPrompt" to "Хотите бережно отложить эту задачу в список 'Отложено'?",
        "moveToCryo" to "Отложить на потом",
        "cancel" to "Оставить в буфере",
        "streamMode" to "Ближайшие",
        "matrixMode" to "Сетка"
    )

    fun get(terminology: Terminology, key: String): String {
        val map = if (terminology == Terminology.HUMAN) humanDict else systemDict
        return map[key] ?: key
    }
}
