package com.paintx.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.Canvas as NativeCanvas
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Mark(val points: List<Offset>, val color: Color, val width: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PaintXApp() }
    }
}

@Composable
private fun PaintXApp() {
    val context = LocalContext.current
    val bg = Color(0xFF171321)
    val panel = Color(0xFF241D32)
    val purple = Color(0xFF9B6DFF)
    val colors = listOf(Color.Black, Color.Red, Color(0xFF2196F3), Color(0xFF20A464), Color(0xFFFFC107), Color(0xFF9C27B0), Color.White)
    var selected by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var eraser by remember { mutableStateOf(false) }
    val marks = remember { mutableStateListOf<Mark>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    MaterialTheme(colorScheme = darkColorScheme(primary = purple, background = bg, surface = panel)) {
        Column(
            Modifier.fillMaxSize().background(bg).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Paint X", color = Color.White, fontSize = 27.sp)
                    Text("ارسم فكرتك", color = Color(0xFFBDA9E8), fontSize = 12.sp)
                }
                Text("V1.0", color = Color(0xFFD8B4FE))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ToolButton("قلم", !eraser) { eraser = false }
                ToolButton("ممحاة", eraser) { eraser = true }
                ToolButton("تراجع", false) { if (marks.isNotEmpty()) marks.removeAt(marks.lastIndex) }
                ToolButton("مسح", false) { marks.clear(); currentPoints = emptyList() }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                colors.forEach { c ->
                    Box(
                        Modifier.size(28.dp).background(c, CircleShape)
                            .then(if (selected == c && !eraser) Modifier.border(3.dp, purple, CircleShape) else Modifier)
                    ) {
                        androidx.compose.foundation.clickable
                    }
                }
            }
            // Color selectors with explicit click targets
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { c ->
                    Box(
                        Modifier.size(30.dp).background(c, CircleShape)
                            .border(if (selected == c && !eraser) 3.dp else 1.dp, if (selected == c && !eraser) purple else Color.Gray, CircleShape)
                            .pointerInput(c) { detectDragGestures(onDragStart = { selected = c; eraser = false }) { _, _ -> } }
                    )
                }
            }

            Text("حجم الفرشاة", color = Color.White)
            Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 2f..30f)

            Box(
                Modifier.weight(1f).fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                    .pointerInput(eraser, selected, brushSize) {
                        detectDragGestures(
                            onDragStart = { currentPoints = listOf(it) },
                            onDrag = { change, _ -> currentPoints = currentPoints + change.position },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    marks.add(Mark(currentPoints, if (eraser) Color.White else selected, brushSize))
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    fun drawMark(mark: Mark) {
                        if (mark.points.size == 1) {
                            drawCircle(mark.color, mark.width / 2, mark.points.first())
                        } else if (mark.points.size > 1) {
                            val p = Path().apply {
                                moveTo(mark.points.first().x, mark.points.first().y)
                                mark.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(p, mark.color, style = Stroke(mark.width, cap = StrokeCap.Round))
                        }
                    }
                    marks.forEach { drawMark(it) }
                    drawMark(Mark(currentPoints, if (eraser) Color.White else selected, brushSize))
                }
            }

            Button(
                onClick = {
                    val size = 1200
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = NativeCanvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val sx = size / 800f
                    marks.forEach { mark ->
                        val path = NativePath()
                        mark.points.forEachIndexed { i, pt ->
                            if (i == 0) path.moveTo(pt.x * sx, pt.y * sx) else path.lineTo(pt.x * sx, pt.y * sx)
                        }
                        val paint = NativePaint().apply {
                            color = mark.color.toArgb()
                            strokeWidth = mark.width * sx
                            style = NativePaint.Style.STROKE
                            strokeCap = NativePaint.Cap.ROUND
                            strokeJoin = NativePaint.Join.ROUND
                            isAntiAlias = true
                        }
                        if (mark.points.size == 1) {
                            canvas.drawCircle(mark.points[0].x * sx, mark.points[0].y * sx, mark.width * sx / 2, paint)
                        } else canvas.drawPath(path, paint)
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "PaintX_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PaintX")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        Toast.makeText(context, "تم حفظ الصورة في Pictures/PaintX", Toast.LENGTH_LONG).show()
                    } else Toast.makeText(context, "تعذر حفظ الصورة", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = purple)
            ) { Text("حفظ PNG") }
        }
    }
}

@Composable
private fun RowScope.ToolButton(label: String, active: Boolean, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFF7045B5) else Color(0xFF382B4D)
        )
    ) { Text(label, fontSize = 12.sp) }
}
