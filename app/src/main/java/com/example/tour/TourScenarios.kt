package com.example.tour

object TourScenarios {

    val DASHBOARD_CORE = TourScenario(
        id = "dashboard_core",
        title = "Архитектура ColdCache",
        steps = listOf(
            TourStep(
                id = "step_header",
                targetId = TourTargetId.SYSTEM_HEADER,
                title = "КОНСОЛЬ УПРАВЛЕНИЯ",
                description = "Индикатор состояния ядра системы, быстрый доступ к Тепловой карте, Мануалу и глобальным режимам.",
                actionHint = "Показывает текущую дату и статус устойчивости.",
                placement = TourPlacement.BOTTOM,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 10f
            ),
            TourStep(
                id = "step_daemons",
                targetId = TourTargetId.MODULAR_DAEMONS,
                title = "МОДУЛЬНЫЕ ДЕМОНЫ",
                description = "Фоновые трекеры привычек (Шаги, Вода, Заряд). Кликните для шага инкремента, или ЗАЖМИТЕ ПАЛЕЦ (Long Click), чтобы открыть тепловую карту активности этого демона.",
                actionHint = "Совет: Долгий тап открывает персональную тепловую карту!",
                placement = TourPlacement.BOTTOM,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 10f
            ),
            TourStep(
                id = "step_buffer",
                targetId = TourTargetId.BUFFER_BUTTON,
                title = "БЫСТРЫЙ БУФЕР (1-LINE CAPTURE)",
                description = "Мгновенный сброс спонтанных мыслей в 1 клик без отвлечения от текущей работы.",
                actionHint = "Карточки в буфере можно свайпать в RAM или Крио.",
                placement = TourPlacement.BOTTOM,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 10f
            ),
            TourStep(
                id = "step_active_ram",
                targetId = TourTargetId.ACTIVE_RAM,
                title = "ОПЕРАТИВНАЯ ПАМЯТЬ (ACTIVE RAM)",
                description = "Фокус дня: держите в работе до 3 задач. При переполнении система предложит разгрузку.",
                actionHint = "Нажмите на задачу для входа в гиперфокус.",
                placement = TourPlacement.BOTTOM,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 12f
            ),
            TourStep(
                id = "step_cryo",
                targetId = TourTargetId.CRYO_STORAGE,
                title = "КРИО-ХРАНИЛИЩЕ (CRYO STORAGE)",
                description = "Глубокая заморозка отложенных задач. Они сохранены, но не создают визуального шума и тревоги.",
                actionHint = "Размораживайте в RAM, когда будете готовы.",
                placement = TourPlacement.TOP,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 12f
            ),
            TourStep(
                id = "step_dev_null",
                targetId = TourTargetId.DEV_NULL,
                title = "СТРОКА /DEV/NULL (ИНСИНЕРАТОР)",
                description = "Сброс сложных, токсичных или навязчивых мыслей вникуда. Напишите текст и отправьте его в пустоту — он растворится в цифровой пыли без следа.",
                actionHint = "Печатайте любые переживания и жмите Enter/Send для их сжигания.",
                placement = TourPlacement.TOP,
                spotlightPaddingDp = 6f,
                spotlightCornerRadiusDp = 10f
            )
        )
    )
}
