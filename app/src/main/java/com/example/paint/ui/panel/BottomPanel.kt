package com.example.paint.ui.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/* ───────────────── общая кнопка-иконка ───────────────── */

@Composable
private fun ToolIconButton(
    icon: ImageVector,
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
