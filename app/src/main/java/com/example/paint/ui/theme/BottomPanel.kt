package com.example.paint.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect



import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

@Composable
fun BottomPanel(
    onClick: (Color) -> Unit,
    onLineWidthChange: (Float) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: (String) -> Unit,
    onEraserClick: () -> Unit,
    onClearAllClick: () -> Unit,
    onPanModeToggle: () -> Unit,
    isPanMode: Boolean
) {
    var showColorPalette by remember { mutableStateOf(false) }
    var showSaveMenu by remember { mutableStateOf(false) }


    var showRgbPicker by remember { mutableStateOf(false) } //RGB
    var customColor by remember { mutableStateOf(Color.Black) }

    // новый стейт для квадрата-палитры
    var showColorPicker by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Основная "плавающая" панель инструментов
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFDFDFD)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Undo
                ToolIconButton(
                    icon = Icons.Default.Undo,
                    contentDescription = "Undo",
                    onClick = onBackClick
                )

                // Палитра/кисть
                ToolIconButton(
                    icon = Icons.Default.Create,
                    contentDescription = "Brush & colors",
                    onClick = {
                        // если сейчас активен режим "рука" — отключаем его
                        if (isPanMode) {
                            onPanModeToggle()
                        }
                        // и открываем/закрываем палитру
                        showColorPalette = !showColorPalette
                    }
                )

                // Ластик
                ToolIconButton(
                    icon = Icons.Default.AutoFixOff,
                    contentDescription = "Eraser",
                    onClick = onEraserClick
                )

                // Режим "рука" (панорамирование)
                ToolIconButton(
                    icon = Icons.Default.PanTool,
                    contentDescription = "Pan mode",
                    onClick = onPanModeToggle,
                    highlighted = isPanMode
                )

                // Стереть всё
                ToolIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Clear all",
                    onClick = onClearAllClick
                )

                // Сохранить (меню PNG/SVG)
                Box {
                    ToolIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Save",
                        onClick = { showSaveMenu = true }
                    )

                    DropdownMenu(
                        expanded = showSaveMenu,
                        onDismissRequest = { showSaveMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сохранить как PNG") },
                            onClick = {
                                showSaveMenu = false
                                onSaveClick("png")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сохранить как SVG") },
                            onClick = {
                                showSaveMenu = false
                                onSaveClick("svg")
                            }
                        )
                    }
                }
            }
        }

        // Палитра + слайдер показываются аккуратно под карточкой
        AnimatedVisibility(visible = showColorPalette) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorList(
                    modifier = Modifier.weight(1f),
                    onClick = { color ->
                        customColor = color
                        onClick(color)
                    },
                    onCustomPaletteClick = { showColorPicker = true }
                )

                Spacer(modifier = Modifier.width(8.dp))

                CustomSlider(
                    modifier = Modifier.weight(1f),
                    onChange = onLineWidthChange
                )
            }
        }

        if (showColorPicker) {
            ColorPaletteDialog(
                initialColor = customColor,
                onColorSelected = { color ->
                    customColor = color
                    onClick(color)          // применяем к кисти
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }


        // Диалог выбора RGB-цвета
        if (showRgbPicker) {
            RgbColorPickerDialog(
                initialColor = customColor,
                onColorSelected = { color ->
                    customColor = color
                    onClick(color)        // применяем выбранный цвет к кисти
                    showRgbPicker = false
                },
                onDismiss = { showRgbPicker = false }
            )
        }

    }
}

/* ───────────────── цветовая палитра ───────────────── */

@Composable
fun ColorList(
    modifier: Modifier = Modifier,
    onClick: (Color) -> Unit,
    onCustomPaletteClick: () -> Unit
) {
    val colors = listOf(
        Color.Black,
        Color.DarkGray,
        Color.Red,
        Color.Magenta,
        Color.Blue,
        Color.Cyan,
        Color.Green,
        Color.Yellow,
        Color.White
    )

    LazyRow(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 🔵 ПЕРВЫЙ круг – палитра
        item {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
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
                        )
                    )
                    .border(1.dp, Color.DarkGray, CircleShape)
                    .clickable { onCustomPaletteClick() }
            )
        }

        // Дальше – обычные фиксированные цвета
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onClick(color) }
            )
        }
    }
}



/* ───────────────── слайдер толщины ───────────────── */

@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    var position by remember { mutableStateOf(0.05f) }

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val sliderWidth = if (isLandscape)
        (configuration.screenWidthDp.dp * 0.5f)
    else
        (configuration.screenWidthDp.dp * 0.3f)

    Column(
        modifier = modifier
            .width(sliderWidth)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Толщина: ${(position * 100).toInt()}")
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = position,
            onValueChange = {
                val tempPos = if (it > 0f) it else 0.01f
                position = tempPos
                onChange(tempPos * 100)
            }
        )
    }
}

/* ───────────────── общая кнопка-иконка ───────────────── */

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val bgColor = if (highlighted) Color(0xFFEEEEEE) else Color.White

    IconButton(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.Black)
    }
}

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



