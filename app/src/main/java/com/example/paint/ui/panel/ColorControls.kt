package com.example.paint.ui.panel

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

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
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
