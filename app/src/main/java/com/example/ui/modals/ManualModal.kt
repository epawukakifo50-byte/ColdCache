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
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Добро пожаловать в ColdCache.OS. Это терминал управления когнитивной нагрузкой.",
                color = colors.textMain,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )

            ManualSection(
                title = "1. ${Dict.get(terminology, "buffer")} (Буфер)",
                desc = "Место для сброса хаоса. Возникла мысль или задача? Быстро записывай её сюда и закрывай терминал. Не держи в голове."
            )

            ManualSection(
                title = "2. ${Dict.get(terminology, "ram")} (Слоты фокуса)",
                desc = "То, что ты делаешь прямо сейчас. Жесткий лимит — 2 слота. Если пытаешься взять третью задачу, система не даст этого сделать, пока не освободишь память (появится предупреждение)."
            )

            ManualSection(
                title = "3. ${Dict.get(terminology, "cryo")} (Отложенное)",
                desc = "Задачи, которые нужно сделать, но не сегодня или не сейчас. Замораживай их здесь, чтобы они не мозолили глаза в фокусе."
            )

            ManualSection(
                title = "4. ${Dict.get(terminology, "render")} (Гиперфокус)",
                desc = "Когда нажимаешь эту кнопку, задача разворачивается на весь экран. Внутри можно создать чек-лист микро-шагов. Если свернуть приложение, состояние рендера сохранится."
            )

            ManualSection(
                title = "5. ${Dict.get(terminology, "temporal")} (Расписание)",
                desc = "Модуль планирования. Позволяет привязать любую задачу из любого модуля к конкретной дате. Отображается в виде матрицы дат (сетка) или в режиме потока (ближайшие)."
            )

            ManualSection(
                title = "6. Daemons (Трекеры)",
                desc = "Три верхние плашки для отслеживания рутины (шаги, вода, что угодно). Настраиваются индивидуально через меню параметров."
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
                    text = "ПОНЯТНО",
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
