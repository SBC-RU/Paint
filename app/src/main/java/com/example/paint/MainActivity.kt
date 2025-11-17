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
    // смещение "камеры" в мировых координатах
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // масштаб
    var scale by remember { mutableStateOf(1f) }

    // временный Path для текущего мазка
    val tempPath = remember { Path() }
    var currentStrokeData by remember { mutableStateOf<PathData?>(null) }

    // просто чтобы Canvas реагировал на движения
    var drawVersion by remember { mutableStateOf(0) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .navigationBarsPadding()
            .clipToBounds()
            .pointerInput(isPanMode, scale) {
                if (isPanMode) {
                    // РЕЖИМ "РУКА": панорамирование + pinch-zoom
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        // pan приходит в экранных координатах → переводим в мировые
                        offsetX += pan.x / scale
                        offsetY += pan.y / scale

                        // масштаб
                        scale = (scale * zoomChange).coerceIn(0.5f, 4f)

                        drawVersion++
                    }
                } else {
                    // РЕЖИМ РИСОВАНИЯ: один палец, без зума/пана
                    detectDragGestures(
                        onDragStart = { start ->
                            tempPath.reset()

                            // экран → мировые координаты
                            val worldX = start.x / scale - offsetX
                            val worldY = start.y / scale - offsetY
                            tempPath.moveTo(worldX, worldY)

                            currentStrokeData = pathData1.value
                            drawVersion++
                        },
                        onDragEnd = {
                            currentStrokeData?.let { data ->
                                // копируем путь в список, чтобы tempPath можно было переиспользовать
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
                        val worldX = change.position.x / scale - offsetX
                        val worldY = change.position.y / scale - offsetY
                        tempPath.lineTo(worldX, worldY)
                        drawVersion++
                    }
                }
            }
    ) {
        // подписываем Canvas на drawVersion
        val dummy = drawVersion

        // сначала масштаб, потом смещение "камеры"
        scale(scale) {
            translate(left = offsetX, top = offsetY) {
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
}



