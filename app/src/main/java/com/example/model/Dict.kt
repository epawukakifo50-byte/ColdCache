package com.example.model

object Dict {
    private val systemDict = mapOf(
        "ram" to "Active RAM",
        "cryo" to "Cryo-Storage",
        "buffer" to "Latent Buffer",
        "archive" to "Crystallization Archive",
        "safeMode" to "System Hibernation",
        "render" to "Render",
        "compile" to "HYPERFOCUS",
        "drop" to "Drop",
        "inject" to "inject_node",
        "manual" to "ReadMe",
        "settings" to "System Settings",
        "emptyMem" to "RAM Slots Free",
        "emptyCryo" to "Cryo Bay Empty",
        "temporal" to "Temporal Flux",
        "overload" to "SYSTEM OVERLOAD",
        "overloadMsg" to "Active RAM slots are occupied (2/2). Cannot insert node:",
        "overloadPrompt" to "Move this task to Cryo-Storage instead?",
        "moveToCryo" to "Move to Cryo",
        "cancel" to "Cancel",
        "streamMode" to "Stream",
        "matrixMode" to "Matrix",
        "flowBattery" to "Flow Battery",
        "subtasks" to "Subtasks",
        "backup" to "Encrypted Backup",
        "daemons" to "Modular Daemons",
        "visual" to "Visual Interface",
        "services" to "System Services",
        "journal" to "Memory Journal"
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
        "manual" to "Инструкция",
        "settings" to "Настройки",
        "emptyMem" to "Слоты внимания свободны",
        "emptyCryo" to "Отложенных задач нет",
        "temporal" to "Календарь",
        "overload" to "Слоты внимания заняты (2/2)",
        "overloadMsg" to "Чтобы сохранить фокус без перегрузки:",
        "overloadPrompt" to "Хотите бережно перенести эту задачу в список 'Отложено'?",
        "moveToCryo" to "Отложить на потом",
        "cancel" to "Оставить в буфере",
        "streamMode" to "Список",
        "matrixMode" to "Сетка",
        "flowBattery" to "Реактор потока",
        "subtasks" to "Подзадачи",
        "backup" to "Локальный бэкап",
        "daemons" to "Модульные трекеры",
        "visual" to "Внешний вид",
        "services" to "Системные службы",
        "journal" to "Журнал памяти"
    )

    fun get(terminology: Terminology, key: String): String {
        val map = if (terminology == Terminology.HUMAN) humanDict else systemDict
        return map[key] ?: key
    }
}
