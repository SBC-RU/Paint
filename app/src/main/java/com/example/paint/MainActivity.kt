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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform

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
                                // любой выбор цвета выключает "руку"
                                isPanMode = false
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = color)
                            },
                            onLineWidthChange = { lineWidth ->
                                // при изменении толщины можно тоже отключать "руку" (по желанию)
                                isPanMode = false
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(lineWidth = lineWidth)
                            },
                            onBackClick = {
                                // при Undo можно тоже сбрасывать "руку", если хочешь
                                // isPanMode = false
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
                                // включили ластик → точно рисуем, а не двигаем холст
                                isPanMode = false
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = Color(0xFFFAFAFA))
                            },
                            onClearAllClick = {
                                // можно тоже сбрасывать "руку"
                                // isPanMode = false
                                viewModel.pathList.clear()
                            },
                            onPanModeToggle = {
                                isPanMode = !isPanMode
                            },
                            isPanMode = isPanMode
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
    // смещение "камеры" в экранных координатах
    var offset by remember { mutableStateOf(Offset.Zero) }

    // масштаб
    var scale by remember { mutableStateOf(1f) }

    // временный Path для текущего мазка
    val tempPath = remember { Path() }
    var currentStrokeData by remember { mutableStateOf<PathData?>(null) }

    // чтобы Canvas реагировал на изменения tempPath/scale/offset
    var drawVersion by remember { mutableStateOf(0) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .navigationBarsPadding()
            .clipToBounds()
            .pointerInput(isPanMode) {
                if (isPanMode) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoomChange).coerceIn(0.5f, 4f)

                        // мировая точка под центром жеста ДО зума
                        val worldBefore = (centroid - offset) / oldScale
                        // хотим, чтобы ПОСЛЕ зума эта же точка осталась под centroid
                        offset = centroid - worldBefore * newScale

                        // плюс pan (он уже в экранных координатах)
                        offset += pan
                        scale = newScale

                        drawVersion++
                    }
                } else {
                    detectDragGestures(
                        onDragStart = { start ->
                            tempPath.reset()
                            val world = (start - offset) / scale
                            tempPath.moveTo(world.x, world.y)
                            currentStrokeData = pathData1.value
                            drawVersion++
                        },
                        onDragEnd = {
                            currentStrokeData?.let { data ->
                                val pathForList = Path().apply { addPath(tempPath) }
                                pathList.add(data.copy(path = pathForList))
                            }
                            currentStrokeData = null
                            drawVersion++
                        },
                        onDragCancel = {
                            currentStrokeData = null
                            drawVersion++
                        }
                    ) { change, _ ->
                        val data = currentStrokeData ?: return@detectDragGestures
                        val world = (change.position - offset) / scale
                        tempPath.lineTo(world.x, world.y)
                        drawVersion++
                    }
                }
            }
    ) {
        // подписываемся на изменения, чтобы Canvas перерисовывался
        val dummyVersion = drawVersion

        withTransform({
            // ✨ ВАЖНО: СНАЧАЛА масштаб, ПОТОМ сдвиг
            scale(scale)
            translate(offset.x, offset.y)
        }) {
            // все завершённые мазки
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

            // текущий незавершённый мазок
            val data = currentStrokeData
            if (data != null) {
                drawPath(
                    tempPath,
                    color = data.color,
                    style = Stroke(
                        data.lineWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }

}




