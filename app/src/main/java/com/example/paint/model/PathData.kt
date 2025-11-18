package com.example.paint.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

//это data класс для сохранения цвета и толщины линий
data class PathData(
    val path: Path = Path(),
    val color: Color = Color.Companion.Blue, //по умолчанию синий
    val lineWidth: Float = 5f //толщина

)