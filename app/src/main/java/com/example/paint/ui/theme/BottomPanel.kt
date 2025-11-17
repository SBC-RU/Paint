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
                    onClick = { showColorPalette = !showColorPalette }
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
                    onClick = onClick,
                    onCustomColorClick = { showRgbPicker = true }
                )

                Spacer(modifier = Modifier.width(8.dp))

                CustomSlider(
                    modifier = Modifier.weight(1f),
                    onChange = onLineWidthChange
                )
            }
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
    onCustomColorClick: () -> Unit
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

        // кружок для кастомного цвета (RGB)
        item {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onCustomColorClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("RGB", style = MaterialTheme.typography.labelSmall)
            }
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

