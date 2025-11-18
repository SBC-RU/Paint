package com.example.paint.export

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
import com.example.paint.model.PathData
import java.io.File
import java.io.FileOutputStream
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.media.MediaScannerConnection


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

        val filename = "paint_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — через MediaStore в Pictures/Paint
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Paint"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri).use { out ->
                    if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw RuntimeException("Не удалось записать PNG в MediaStore")
                    }
                }

                // снимаем флаг "IS_PENDING"
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Toast.makeText(context, "PNG сохранён в Pictures/Paint", Toast.LENGTH_LONG).show()
                Log.d(TAG, "PNG saved to MediaStore, uri=$uri")
            } else {
                throw RuntimeException("resolver.insert вернул null")
            }
        } else {
            // Android 9 и ниже — старый добрый публичный Pictures
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // чтобы система «подхватила» новую картинку
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )

            Toast.makeText(
                context,
                "PNG сохранён:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "PNG saved to ${file.absolutePath}")
        }
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

        val filename = "paint_${System.currentTimeMillis()}.svg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — пишем в Downloads/PaintSvg через MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "image/svg+xml")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/PaintSvg"
                )
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                resolver.openOutputStream(uri).use { out ->
                    if (out == null) throw RuntimeException("Не удалось открыть OutputStream")
                    out.write(sb.toString().toByteArray())
                }

                Toast.makeText(
                    context,
                    "SVG сохранён в Загрузки/PaintSvg",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "SVG saved to MediaStore (Downloads), uri=$uri")
            } else {
                throw RuntimeException("resolver.insert для SVG вернул null")
            }
        } else {
            // Android 9 и ниже — публичный каталог Downloads
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/svg+xml"),
                null
            )

            Toast.makeText(
                context,
                "SVG сохранён:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "SVG saved to ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка при сохранении SVG", e)
        Toast.makeText(context, "Ошибка сохранения SVG", Toast.LENGTH_SHORT).show()
    }
}
