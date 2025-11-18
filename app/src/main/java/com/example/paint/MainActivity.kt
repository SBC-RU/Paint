package com.example.paint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.paint.ui.panel.BottomPanel
import com.example.paint.ui.theme.PaintTheme
import androidx.compose.ui.graphics.Color
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext

import android.util.DisplayMetrics
import com.example.paint.export.saveDrawingAsPng
import com.example.paint.export.saveDrawingAsSvg
import com.example.paint.ui.canvas.PaintCanvas
import com.example.paint.viewmodel.MainViewModel

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

            //var isPanMode by remember { mutableStateOf(false) }  //

            PaintTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    PaintCanvas(
                        pathData1 = viewModel.currentPathData,
                        pathList = viewModel.pathList,
                        isPanMode = viewModel.isPanMode
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    ) {
                        BottomPanel(
                            currentColor = viewModel.currentPathData.value.color,   // ← добавим параметр
                            onClick = { color ->
                                viewModel.disablePanMode()
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = color)
                            },
                            onLineWidthChange = { lineWidth ->
                                viewModel.disablePanMode()
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
                                viewModel.disablePanMode()
                                viewModel.currentPathData.value =
                                    viewModel.currentPathData.value.copy(color = Color(0xFFFAFAFA))
                            },
                            onClearAllClick = {
                                viewModel.pathList.clear()
                            },
                            onPanModeToggle = {
                                viewModel.togglePanMode()
                            },
                            isPanMode = viewModel.isPanMode
                        )

                    }
                }
            }
        }
    }
}
