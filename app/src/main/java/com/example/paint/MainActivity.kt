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

            // размеры экрана в px (для экспорта)
            val displayMetrics: DisplayMetrics = context.resources.displayMetrics
            val widthPx = displayMetrics.widthPixels
            val heightPx = displayMetrics.heightPixels

            PaintTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    PaintCanvas(viewModel.currentPathData, viewModel.pathList)

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    ) {
                        BottomPanel(
                            { color ->
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = color)
                            },
                            { lineWidth ->
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(lineWidth = lineWidth)
                            },
                            {
                                if (viewModel.pathList.isNotEmpty()) {
                                    val last = viewModel.pathList.last()
                                    viewModel.pathList.removeIf { it == last }
                                }
                            },
                            { format ->
                                when (format) {
                                    "png" -> {
                                        saveDrawingAsPng(context, viewModel.pathList, widthPx, heightPx)
                                    }
                                    "svg" -> {
                                        saveDrawingAsSvg(context, viewModel.pathList, widthPx, heightPx)
                                    }
                                }
                            },

                            {
                                // Ластик
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = Color(0xFFFAFAFA))
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PaintCanvas(pathData1: MutableState<PathData>, pathList: SnapshotStateList<PathData>) {
    var tempPath = Path()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .navigationBarsPadding()
            .clipToBounds()
            .pointerInput(true) {
                detectDragGestures(
                    onDragStart = {
                        tempPath = Path()
                    },
                    onDragEnd = {
                        pathList.add(
                            pathData1.value.copy(
                                path = tempPath
                            )
                        )

                    }
                ) { change, dragAmount ->
                    tempPath.moveTo(
                        change.position.x - dragAmount.x,
                        change.position.y - dragAmount.y
                    )
                    tempPath.lineTo(
                        change.position.x,
                        change.position.y
                    )

                    if (pathList.size > 0) { //0 было, 1 стало.
                        //фикс бага #1
                        //pathList.removeAt(pathList.size - 1)
                    }

                    pathList.add(
                        pathData1.value.copy(
                            path = tempPath
                        )
                    )
                }
            }
    ) {
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
