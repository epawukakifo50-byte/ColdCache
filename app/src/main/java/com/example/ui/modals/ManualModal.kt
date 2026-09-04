package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dict
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes

@Composable
fun ManualModal(
    terminology: Terminology,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent background pass-through */ }
            .testTag("manual_modal")
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgHeader)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Dict.get(terminology, "manual").uppercase(),
                    color = colors.textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(shapes.secondary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                    .clickable {
                        com.example.util.AppHaptics.click(context)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- Body Content ---
        val isSystem = terminology == Terminology.SYSTEM
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.primary)
                    .padding(14.dp)
            ) {
                Text(
                    text = if (isSystem) {
                        "ColdCache.OS is a cognitive load management system built for high-demand focus, ADHD-friendly execution, and preventing decision paralysis."
                    } else {
                        "ColdCache.OS — это система управления когнитивной нагрузкой и вниманием. Она создана для защиты от перегрузки, снятия тревожности и легкого входа в глубокий фокус."
                    },
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }

            ManualSection(
                title = if (isSystem) "1. LATENT BUFFER (INBOX ZERO)" else "1. БЫСТРЫЙ БУФЕР (ВХОДЯЩИЕ)",
                desc = if (isSystem) {
                    "Instant capture scratchpad for raw thoughts. Write a single line and tap Inject. Do not keep tasks in your head.\n\n" +
                    "• Swipe Right → Move node directly to Active RAM.\n" +
                    "• Swipe Left → Freeze node to Cryo Storage."
                } else {
                    "Место мгновенного сброса спонтанных мыслей. Пришла идея или задача? Запиши её одной строкой и закрой терминал. Не держи мысли в голове.\n\n" +
                    "• Свайп вправо → Отправить задачу сразу в Оперативную память (RAM).\n" +
                    "• Свайп влево → Заморозить задачу в Крио-хранилище."
                }
            )

            ManualSection(
                title = if (isSystem) "2. ACTIVE RAM (FOCUS SLOTS)" else "2. ОПЕРАТИВНАЯ ПАМЯТЬ (ФОКУС ДНЯ)",
                desc = if (isSystem) {
                    "Working memory slots. Strict limit of 2-3 concurrent nodes. Protects your brain from context switching and anxiety."
                } else {
                    "Слоты внимания на сегодня. Строгий лимит — не более 2–3 задач одновременно. Защищает мозг от распыления внимания, синдрома «браться за всё сразу» и переутомления."
                }
            )

            ManualSection(
                title = if (isSystem) "3. CRYO-STORAGE (DEFERRED QUEUE)" else "3. КРИО-ХРАНИЛИЩЕ (ОТЛОЖЕНО)",
                desc = if (isSystem) {
                    "Deep freeze for backlog tasks. They are safely preserved but produce zero visual noise until you are ready."
                } else {
                    "Глубокая заморозка непервоочередных задач. Они надёжно сохранены, но не мозолят глаза и не создают чувства вины. Размораживайте в RAM, когда освободится слот."
                }
            )

            ManualSection(
                title = if (isSystem) "4. RENDER & FLOW BATTERY (HYPERFOCUS)" else "4. РЕЖИМ РЕНДЕРА И КВАНТОВЫЙ РЕАКТОР",
                desc = if (isSystem) {
                    "Full-screen execution terminal that cuts off all app chrome and notifications.\n\n" +
                    "• Flow Battery & Quanta: Every 1 second of holding focus generates 1 Quantum of energy. 4 battery tiers fill up progressively (15 / 30 / 45 / 60+ min) with distinct tonal gradients (Cyan → Lime → Amber → Pink).\n" +
                    "• Focus Protocols: 3-Minute Micro-Probe (ignites dopamine to overcome starting paralysis), Full Sprint, and Cooling Pitstops.\n" +
                    "• Micro-Checklist: Subtasks broken down into actionable steps. Keeps your state visible above the keyboard.\n" +
                    "• Thermal Dissipation: On completion, cooling HUD smoothly releases excess tension."
                } else {
                    "Полноэкранный терминал глубокой работы, отсекающий весь визуальный шум интерфейса.\n\n" +
                    "• Квантовый реактор (Flow Battery): Каждая секунда удержания внимания вырабатывает 1 неделимый квант энергии. 4 деления батареи (15 / 30 / 45 / 60+ мин) последовательно заполняются своими тонами (Циан → Лайм → Янтарный → Розовый).\n" +
                    "• Протоколы старта: 3-минутный зонд (Микро-старт для быстрого обхода страха сложной задачи), Спринт и Пит-стоп.\n" +
                    "• Чек-лист микро-шагов: Дробление задачи на элементарные действия прямо в процессе работы со скроллом над клавиатурой.\n" +
                    "• Охлаждение (Thermal Dissipation): При завершении задачи неоновый HUD плавно снимает остаточное напряжение."
                }
            )

            ManualSection(
                title = if (isSystem) "5. CRYSTALLIZATION & HEATMAP MATRIX" else "5. КРИСТАЛЛИЗАЦИЯ И ТЕПЛОВАЯ КАРТА",
                desc = if (isSystem) {
                    "• Why Crystallization: Completed tasks are never destroyed — they crystallize into permanent historical memory.\n" +
                    "• Heatmap Matrix: A GitHub-style activity grid across days and hours. Higher focus intensity and completed quanta glow with brighter radiance. Gives you an honest, empowering view of your consistency without guilt."
                } else {
                    "• Зачем нужна Кристаллизация: Выполненные задачи не стираются, а «кристаллизуются» в постоянный архив системы.\n" +
                    "• Тепловая карта (Heatmap Matrix): Наглядная матрица активности по дням и часам (как на GitHub). Чем интенсивнее вы закрывали задачи и держали фокус, тем ярче сияет ячейка дня. Позволяет объективно видеть свой прогресс без самокритики."
                }
            )

            ManualSection(
                title = if (isSystem) "6. TEMPORAL FLUX (SCHEDULER ENGINE)" else "6. ТЕМПОРАЛ ФЛАКС (ПЛАНИРОВЩИК ВРЕМЕНИ)",
                desc = if (isSystem) {
                    "Spatio-temporal scheduling engine for tasks tied to dates and times.\n\n" +
                    "• Temporal Stream: Chronological stream grouped by days.\n" +
                    "• Matrix Grid: Interactive calendar grid displaying workload density badges.\n" +
                    "Schedule deferred tasks into the future to keep current RAM clear."
                } else {
                    "Движок привязки задач к конкретным датам и часам.\n\n" +
                    "• Temporal Stream: Хронологическая лента запланированных задач с группировкой по дням.\n" +
                    "• Matrix Grid: Календарная сетка с цветовыми индикаторами плотности нагрузки на каждый день.\n" +
                    "Позволяет отложить задачу на нужную дату без перегрузки текущей оперативной памяти."
                }
            )

            ManualSection(
                title = if (isSystem) "7. /DEV/NULL INCINERATOR (MENTAL PURGE)" else "7. ИНСИНЕРАТОР /DEV/NULL (СБРОС МЫСЛЕЙ)",
                desc = if (isSystem) {
                    "Psychological purge terminal. Write down heavy, toxic, obsessive thoughts or dead-end ideas and hit Enter.\n\n" +
                    "Text is irrevocably vaporized into digital dust. It is NOT saved in any database or log. Pure cathartic release into the void."
                } else {
                    "Строка психологической разгрузки. Сюда можно выписать любые тяжелые, токсичные, тревожные мысли или навязчивые идеи и нажать Отправить.\n\n" +
                    "Текст необратимо сгорает в цифровой пыли. Он НЕ сохраняется ни в какие логи и базы данных. Это чистое освобождение сознания в пустоту."
                }
            )

            ManualSection(
                title = if (isSystem) "8. MODULAR DAEMONS & PERSONAL HEATMAPS" else "8. МОДУЛЬНЫЕ ДЕМОНЫ И ТЕПЛОВЫЕ КАРТЫ",
                desc = if (isSystem) {
                    "Background habit and metric trackers pinned to the top header.\n\n" +
                    "• Quick Tap: Increments value by step.\n" +
                    "• LONG PRESS: Opens dedicated historical heatmap for that specific daemon to inspect your long-term streak.\n" +
                    "Configure habits, colors, targets, and step sizes in Settings."
                } else {
                    "Верхние плашки фоновых привычек и метрик (Шаги, Вода, Заряд, Медитация).\n\n" +
                    "• Обычный клик: Увеличивает счетчик на 1 шаг.\n" +
                    "• ЗАЖАТЬ ПАЛЕЦ (LONG CLICK): Открывает персональную тепловую карту активности конкретного демона за недели для анализа привычки.\n" +
                    "Настраивайте цели, цвета, иконки и величину шага в меню Настроек."
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // --- Bottom Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgHeader)
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.primary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.primary)
                    .clickable {
                        com.example.util.AppHaptics.click(context)
                        onClose()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSystem) "ACKNOWLEDGED" else "ПОНЯТНО",
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun ManualSection(title: String, desc: String) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.primary)
            .background(colors.bgPanel.copy(alpha = 0.5f))
            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = colors.accent1,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Text(
            text = desc,
            color = colors.textMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}
