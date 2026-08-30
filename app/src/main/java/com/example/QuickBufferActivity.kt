package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Input
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.local.toEntity
import com.example.model.Task
import com.example.model.TaskState
import com.example.ui.theme.ColdCacheTheme
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import com.example.util.AppHaptics
import com.example.widget.ColdCacheWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickBufferActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefManager = PreferenceManager(applicationContext)
        val config = prefManager.loadSystemConfig()

        setContent {
            ColdCacheTheme(config = config) {
                val colors = LocalColdCacheColors.current
                val shapes = LocalColdCacheShapes.current
                val context = LocalContext.current
                var inputText by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                }

                fun submitBufferDump() {
                    val trimmed = inputText.trim()
                    if (trimmed.isEmpty()) return
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(applicationContext)
                        val newTask = Task(
                            title = trimmed,
                            state = TaskState.BUFFER
                        )
                        db.taskDao().insertTask(newTask.toEntity())
                        ColdCacheWidgetProvider.updateAllWidgets(applicationContext)

                        withContext(Dispatchers.Main) {
                            AppHaptics.success(context, config.hapticFeedbackEnabled)
                            Toast.makeText(applicationContext, "✓ [ BUFFER ] Thought dumped", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlow(colors.accent1, colors.glowLevel, radius = 16.dp)
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(1.dp, colors.accent1.copy(alpha = 0.6f), shapes.primary)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Prevent dismissing when clicking card */ }
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Input,
                                        contentDescription = null,
                                        tint = colors.accent1,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "QUICK BUFFER DUMP",
                                        color = colors.accent1,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { finish() }
                                )
                            }

                            Text(
                                text = "Киньте мысль без веса и сроков. Разберете потом, когда будет ресурс.",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Quick 1-line text input
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                singleLine = false,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { submitBufferDump() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .clip(shapes.secondary)
                                    .background(colors.bgBase)
                                    .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                    .padding(12.dp)
                            )

                            // Bottom Action: DUMP BUTTON
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.secondary)
                                        .background(colors.accentBrush)
                                        .clickable { submitBufferDump() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DUMP // В БУФЕР",
                                        color = colors.bgBase,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
