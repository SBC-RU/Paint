package com.example.paint.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.paint.model.PathData

class MainViewModel : ViewModel() {
    // Список всех нарисованных линий
    val pathList = mutableStateListOf<PathData>()

    // Текущие параметры рисования (цвет, ширина, path)
    val currentPathData = mutableStateOf(PathData())
}