package com.example.paint.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.paint.model.PathData

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
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val oldScale = scale

                        // панорамирование: pan приходит в экранных координатах → переводим в world
                        offset += pan / oldScale

                        // зум просто меняем
                        scale = (scale * zoomChange).coerceIn(0.5f, 4f)

                        drawVersion++
                    }
                } else {

                    detectDragGestures(
                        onDragStart = { start ->
                            tempPath.reset()
                            // экран → мир: world = screen / scale - offset
                            val world = start / scale - offset
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
                        val world = change.position / scale - offset
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
