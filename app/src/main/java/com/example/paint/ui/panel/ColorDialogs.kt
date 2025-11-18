package com.example.paint.ui.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures

@Composable
fun RgbColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var r by remember { mutableStateOf(initialColor.red) }   // 0f..1f
    var g by remember { mutableStateOf(initialColor.green) }
    var b by remember { mutableStateOf(initialColor.blue) }

    val previewColor = Color(r, g, b)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onColorSelected(previewColor) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Выбор цвета (RGB)") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // превью выбранного цвета
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(previewColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("R: ${(r * 255).toInt()}")
                Slider(
                    value = r,
                    onValueChange = { r = it }
                )

                Text("G: ${(g * 255).toInt()}")
                Slider(
                    value = g,
                    onValueChange = { g = it }
                )

                Text("B: ${(b * 255).toInt()}")
                Slider(
                    value = b,
                    onValueChange = { b = it }
                )
            }
        }
    )
}

@Composable
fun ColorPaletteDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }    // 0..360
    var value by remember { mutableStateOf(1f) }  // 0..1 (яркость)
    var selectorPos by remember { mutableStateOf(Offset.Zero) }
    var paletteSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        value = hsv[2]
    }

    val selectedColor = Color.hsv(hue, 1f, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        // ВАЖНО: title убираем, всё рисуем в text
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок внутри, всегда видно
                Text(
                    text = "Выбор цвета",
                    style = MaterialTheme.typography.titleMedium
                )

                // Квадрат палитры, с ограничением высоты
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)     // фиксируем максимум по высоте
                        .aspectRatio(1f)            // стараемся держать квадрат
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val w = paletteSize.width.coerceAtLeast(1f)
                                val h = paletteSize.height.coerceAtLeast(1f)
                                val x = change.position.x.coerceIn(0f, w)
                                val y = change.position.y.coerceIn(0f, h)
                                selectorPos = Offset(x, y)

                                hue = (x / w) * 360f
                                value = 1f - (y / h)
                            }
                        }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        paletteSize = size

                        // радуга по горизонтали
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            ),
                            size = size
                        )

                        // затемнение вниз
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black)
                            ),
                            size = size
                        )

                        val px = if (selectorPos == Offset.Zero)
                            (hue / 360f).coerceIn(0f, 1f) * size.width
                        else selectorPos.x

                        val py = if (selectorPos == Offset.Zero)
                            (1f - value).coerceIn(0f, 1f) * size.height
                        else selectorPos.y

                        val markerCenter = Offset(px, py)
                        val radius = 8.dp.toPx()

                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = markerCenter,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // превью выбранного цвета
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                )
            }
        }
    )
}
