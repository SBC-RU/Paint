package com.example.paint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PathMeasure
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.example.paint.ui.theme.PathData
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PaintExport"

/**
 * Сохранение рисунка в PNG в папку приложения:
 * /Android/data/<твой.пакет>/files/Pictures
 */
fun saveDrawingAsPng(
    context: Context,
    pathList: List<PathData>,
    widthPx: Int,
    heightPx: Int,
    backgroundColor: Color = Color(0xFFFAFAFA)
) {
    if (pathList.isEmpty()) {
        Toast.makeText(context, "Нечего сохранять", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        // фон
        canvas.drawColor(backgroundColor.toArgb())

        val paint = AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
        }

        pathList.forEach { pathData ->
            val androidPath = pathData.path.asAndroidPath()
            paint.color = pathData.color.toArgb()
            paint.strokeWidth = pathData.lineWidth
            canvas.drawPath(androidPath, paint)
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (dir == null) {
            Toast.makeText(context, "Не удалось получить папку для картинок", Toast.LENGTH_SHORT)
                .show()
            Log.e(TAG, "getExternalFilesDir(Environment.DIRECTORY_PICTURES) == null")
            return
        }

        if (!dir.exists()) dir.mkdirs()

        val filename = "paint_${System.currentTimeMillis()}.png"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        Toast.makeText(
            context,
            "PNG сохранён:\n${file.absolutePath}",
            Toast.LENGTH_LONG
        ).show()

        Log.d(TAG, "PNG saved to ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка при сохранении PNG", e)
        Toast.makeText(context, "Ошибка сохранения PNG", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Конвертация Path в SVG path-d через дискретизацию
 */
private fun pathToSvgPath(path: Path, step: Float = 5f): String {
    val androidPath = path.asAndroidPath()
    val pm = PathMeasure(androidPath, false)

    val pos = FloatArray(2)
    val sb = StringBuilder()

    do {
        val length = pm.length
        if (length == 0f) continue

        var distance = 0f
        var firstPoint = true

        while (distance <= length) {
            val ok = pm.getPosTan(distance, pos, null)
            if (ok) {
                val x = pos[0]
                val y = pos[1]
                if (firstPoint) {
                    sb.append("M $x $y ")
                    firstPoint = false
                } else {
                    sb.append("L $x $y ")
                }
            }
            distance += step
        }
    } while (pm.nextContour())

    return sb.toString().trim()
}

private fun colorToSvg(color: Color): String {
    val argb = color.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

/**
 * Сохранение рисунка в SVG в папку приложения:
 * /Android/data/<твой.пакет>/files/Documents
 */
fun saveDrawingAsSvg(
    context: Context,
    pathList: List<PathData>,
    widthPx: Int,
    heightPx: Int
) {
    if (pathList.isEmpty()) {
        Toast.makeText(context, "Нечего сохранять", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val sb = StringBuilder()
        sb.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="$widthPx" height="$heightPx" viewBox="0 0 $widthPx $heightPx">"""
        )

        pathList.forEach { pathData ->
            val d = pathToSvgPath(pathData.path)
            if (d.isNotEmpty()) {
                val color = colorToSvg(pathData.color)
                sb.append(
                    """<path d="$d" fill="none" stroke="$color" stroke-width="${pathData.lineWidth}" stroke-linecap="round" />"""
                )
            }
        }

        sb.append("</svg>")

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir == null) {
            Toast.makeText(context, "Не удалось получить папку для документов", Toast.LENGTH_SHORT)
                .show()
            Log.e(TAG, "getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) == null")
            return
        }

        if (!dir.exists()) dir.mkdirs()

        val filename = "paint_${System.currentTimeMillis()}.svg"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            out.write(sb.toString().toByteArray())
        }

        Toast.makeText(
            context,
            "SVG сохранён:\n${file.absolutePath}",
            Toast.LENGTH_LONG
        ).show()

        Log.d(TAG, "SVG saved to ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка при сохранении SVG", e)
        Toast.makeText(context, "Ошибка сохранения SVG", Toast.LENGTH_SHORT).show()
    }
}
