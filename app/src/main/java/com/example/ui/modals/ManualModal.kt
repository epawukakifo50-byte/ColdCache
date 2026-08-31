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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isSystem) "Welcome to ColdCache.OS — cognitive load management terminal." else "Добро пожаловать в ColdCache.OS. Это терминал управления когнитивной нагрузкой.",
                color = colors.textMain,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )

            ManualSection(
                title = if (isSystem) "1. LATENT BUFFER" else "1. ВХОДЯЩИЕ МЫСЛИ",
                desc = if (isSystem) "Cognitive scratchpad. Have a raw thought or task? Inject it instantly and clear your mind." else "Место для сброса мыслей. Возникла мысль или задача? Быстро записывай её сюда и закрывай терминал. Не держи в голове."
            )

            ManualSection(
                title = if (isSystem) "2. ACTIVE RAM" else "2. В ФОКУСЕ (Слоты внимания)",
                desc = if (isSystem) "High-priority execution. Strict 2-slot limit. Prevent cognitive overload." else "То, что ты делаешь прямо сейчас. Жесткий лимит — 2 слота. Защита от перегрузки."
            )

            ManualSection(
                title = if (isSystem) "3. CRYO-STORAGE" else "3. ОТЛОЖЕНО",
                desc = if (isSystem) "Deferred tasks. Freeze low-urgency tasks to maintain laser focus on active nodes." else "Задачи, которые нужно сделать, но не сегодня или не сейчас. Замораживай их здесь, чтобы не отвлекали."
            )

            ManualSection(
                title = if (isSystem) "4. RENDER & FLOW BATTERY" else "4. НАЧАТЬ И РЕАКТОР ПОТОКА",
                desc = if (isSystem) "Fullscreen hyperfocus mode with micro-step checklists and quantum energy tracker." else "Полноэкранный режим фокуса с чек-листом подзадач и трекером глубокой работы."
            )

            ManualSection(
                title = if (isSystem) "5. TEMPORAL FLUX" else "5. КАЛЕНДАРЬ И ВРЕМЯ",
                desc = if (isSystem) "Scheduling engine with Stream (chronological) and Matrix (calendar grid) views." else "Модуль планирования. Привязка задач к дате, просмотр в виде списка или календаря."
            )

            ManualSection(
                title = if (isSystem) "6. MODULAR DAEMONS" else "6. МОДУЛЬНЫЕ ТРЕКЕРЫ",
                desc = if (isSystem) "Customizable background trackers for steps, water, habits, and metrics." else "Верхние плашки для отслеживания рутины (шаги, вода, привычки). Настраиваются в меню параметров."
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = colors.accent1,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = desc,
            color = colors.textMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 17.sp
        )
    }
}
