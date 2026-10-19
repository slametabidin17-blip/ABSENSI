package com.example.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.ui.theme.AmberLate
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess

@Composable
fun FaceBoundingBoxOverlay(
    faces: List<CameraXAttendanceAnalyzer.FaceData> = emptyList(),
    qrCodes: List<CameraXAttendanceAnalyzer.QrData> = emptyList(),
    frameWidth: Int,
    frameHeight: Int,
    isFrontCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (frameWidth <= 0 || frameHeight <= 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / frameWidth
        val scaleY = canvasHeight / frameHeight

        // 1. Gambar Bounding Box Wajah
        for (face in faces) {
            val bb = face.boundingBox

            val left = if (isFrontCamera) {
                canvasWidth - (bb.right * scaleX)
            } else {
                bb.left * scaleX
            }
            val right = if (isFrontCamera) {
                canvasWidth - (bb.left * scaleX)
            } else {
                bb.right * scaleX
            }
            val top = bb.top * scaleY
            val bottom = bb.bottom * scaleY

            val boxWidth = (right - left).coerceAtLeast(10f)
            val boxHeight = (bottom - top).coerceAtLeast(10f)

            val isVerified = face.matchedMember != null && face.matchConfidence >= 0.85
            val boxColor = if (isVerified) GreenSuccess else CyanAccent

            // Bounding rounded box
            drawRoundRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            // Corner reticle accents
            val cornerLen = 24f
            val strokeW = 6f

            drawLine(boxColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(boxColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

            drawLine(boxColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
            drawLine(boxColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)

            drawLine(boxColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
            drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

            drawLine(boxColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
            drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

            // Text label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 34f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                val bgPaint = android.graphics.Paint().apply {
                    color = if (isVerified) 0xCC10B981.toInt() else 0xCC0284C7.toInt()
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }

                val memberName = face.matchedMember?.name ?: "Memindai Wajah..."
                val confidencePct = (face.matchConfidence * 100).toInt()
                val text = "$memberName ($confidencePct%)"
                val textWidth = paint.measureText(text)

                val labelX = left.coerceAtLeast(10f)
                val labelY = (top - 18f).coerceAtLeast(40f)

                drawRoundRect(
                    labelX - 10f,
                    labelY - 38f,
                    labelX + textWidth + 14f,
                    labelY + 12f,
                    8f,
                    8f,
                    bgPaint
                )
                drawText(text, labelX, labelY, paint)
            }
        }

        // 2. Gambar Bounding Box QR Code / Barcode
        for (qr in qrCodes) {
            val bb = qr.boundingBox ?: continue

            val left = if (isFrontCamera) {
                canvasWidth - (bb.right * scaleX)
            } else {
                bb.left * scaleX
            }
            val right = if (isFrontCamera) {
                canvasWidth - (bb.left * scaleX)
            } else {
                bb.right * scaleX
            }
            val top = bb.top * scaleY
            val bottom = bb.bottom * scaleY

            val boxWidth = (right - left).coerceAtLeast(20f)
            val boxHeight = (bottom - top).coerceAtLeast(20f)

            val isRecognized = qr.matchedMember != null
            val qrColor = if (isRecognized) GreenSuccess else AmberLate

            // Kotak target QR solid neon
            drawRoundRect(
                color = qrColor,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 6f)
            )

            // Aksen sudut QR
            val cornerLen = 30f
            val strokeW = 8f

            drawLine(qrColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(qrColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

            drawLine(qrColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
            drawLine(qrColor, Offset(right, top), Offset(right, top - cornerLen), strokeW)

            drawLine(qrColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
            drawLine(qrColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

            drawLine(qrColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
            drawLine(qrColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

            // Label QR
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                val bgPaint = android.graphics.Paint().apply {
                    color = if (isRecognized) 0xEE059669.toInt() else 0xEED97706.toInt()
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }

                val title = if (isRecognized) {
                    "✓ QR: ${qr.matchedMember?.name} (${qr.matchedMember?.identifier})"
                } else {
                    "QR: ${qr.rawValue} (Belum Terdaftar)"
                }

                val textWidth = paint.measureText(title)
                val labelX = left.coerceAtLeast(10f)
                val labelY = (bottom + 42f).coerceAtMost(canvasHeight - 20f)

                drawRoundRect(
                    labelX - 10f,
                    labelY - 36f,
                    labelX + textWidth + 14f,
                    labelY + 12f,
                    8f,
                    8f,
                    bgPaint
                )
                drawText(title, labelX, labelY, paint)
            }
        }
    }
}
