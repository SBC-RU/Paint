package com.example.paint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.paint.ui.theme.BottomPanel
import com.example.paint.ui.theme.PaintTheme
import com.example.paint.ui.theme.PathData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import com.example.paint.MainViewModel
import androidx.compose.ui.graphics.drawscope.translate

import androidx.compose.ui.platform.LocalContext
import android.util.DisplayMetrics
import androidx.compose.runtime.CompositionLocalProvider
// ...остальные импорты

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val viewModel: MainViewModel by viewModels()
            val context = LocalContext.current

            val displayMetrics: DisplayMetrics = context.resources.displayMetrics
            val widthPx = displayMetrics.widthPixels
            val heightPx = displayMetrics.heightPixels

            var isPanMode by remember { mutableStateOf(false) }  // ← НОВОЕ

            PaintTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    PaintCanvas(
                        pathData1 = viewModel.currentPathData,
                        pathList = viewModel.pathList,
                        isPanMode = isPanMode                   // ← ПЕРЕДАЁМ
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    ) {
                        BottomPanel(
                            onClick = { color ->
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = color)
                            },
                            onLineWidthChange = { lineWidth ->
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(lineWidth = lineWidth)
                            },
                            onBackClick = {
                                if (viewModel.pathList.isNotEmpty()) {
                                    val last = viewModel.pathList.last()
                                    viewModel.pathList.removeIf { it == last }
                                }
                            },
                            onSaveClick = { format ->
                                when (format) {
                                    "png" -> saveDrawingAsPng(context, viewModel.pathList, widthPx, heightPx)
                                    "svg" -> saveDrawingAsSvg(context, viewModel.pathList, widthPx, heightPx)
                                }
                            },
                            onEraserClick = {
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = Color(0xFFFAFAFA))
                            },
                            onClearAllClick = {
                                viewModel.pathList.clear()
                            },
                            onPanModeToggle = {
                                isPanMode = !isPanMode                // ← ПЕРЕКЛЮЧАЕМ РЕЖИМ
                            },
                            isPanMode = isPanMode                     // ← ЧТОБЫ КНОПКА МЕНЯЛА ВИД/ЦВЕТ
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PaintCanvas(
    pathData1: MutableState<PathData>,
    pathList: SnapshotStateList<PathData>,
    isPanMode: Boolean
) {
    var tempPath by remember { mutableStateOf(Path()) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }


    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .navigationBarsPadding()
            .clipToBounds()
            .pointerInput(isPanMode) {
                detectDragGestures(
                    onDragStart = { _ ->
                        if (!isPanMode) {
                            tempPath = Path()
                        }
                    },
                    onDragEnd = {
                        if (!isPanMode) {
                            pathList.add(
                                pathData1.value.copy(
                                    path = tempPath
                                )
                            )
                        }
                    }
                ) { change, dragAmount ->
                    if (isPanMode) {
                        // Режим "рука" — двигаем холст
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    } else {
                        // Обычный режим — рисуем, но учитываем смещение холста
                        val startX = change.position.x - dragAmount.x - offsetX
                        val startY = change.position.y - dragAmount.y - offsetY
                        val endX = change.position.x - offsetX
                        val endY = change.position.y - offsetY

                        tempPath.moveTo(startX, startY)
                        tempPath.lineTo(endX, endY)

                        pathList.add(
                            pathData1.value.copy(
                                path = tempPath
                            )
                        )
                    }
                }
            }
    ) {
        // Рисуем с учетом смещения холста
        translate(left = offsetX, top = offsetY) {
            pathList.forEach { pathData ->
                drawPath(
                    pathData.path,
                    color = pathData.color,
                    style = Stroke(
                        pathData.lineWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

    }

}
