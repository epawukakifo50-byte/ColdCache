package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevNullConsole(
    modifier: Modifier = Modifier
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    var input by remember { mutableStateOf("") }
    var isFading by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (isFading) 0f else 1f,
        animationSpec = tween(durationMillis = 800),
        label = "devNullAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFading) 0.92f else 1f,
        animationSpec = tween(durationMillis = 800),
        label = "devNullScale"
    )

    fun submit() {
        if (input.isNotBlank() && !isFading) {
            com.example.util.AppHaptics.dumpRelease(context)
            isFading = true
            coroutineScope.launch {
                delay(800)
                input = ""
                isFading = false
                focusManager.clearFocus()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.primary)
            .background(colors.bgHeader)
            .border(1.dp, colors.borderStrong, shapes.primary)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("dev_null_console")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = "DevNull",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha)
                    .scale(scale)
            ) {
                if (input.isEmpty()) {
                    Text(
                        text = "/dev/null (сброс мыслей)...",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = { if (!isFading) input = it },
                    textStyle = TextStyle(
                        color = colors.textMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    cursorBrush = SolidColor(colors.accent1),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { submit() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
