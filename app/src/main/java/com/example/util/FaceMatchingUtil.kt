package com.example.util

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

object FaceMatchingUtil {

    /**
     * Menghasilkan signature fitur wajah sederhana berdasarkan profil dan karakteristik warna/kontras.
     */
    fun generateFaceSignature(name: String, identifier: String, bitmap: Bitmap?): String {
        val baseHash = "${name.lowercase().trim()}|${identifier.trim()}".hashCode()
        if (bitmap == null) {
            return "FACE_SIG_${abs(baseHash)}"
        }

        // Sampling 16 titik pada bitmap untuk menciptakan representasi signature visual
        val width = bitmap.width
        val height = bitmap.height
        val sb = StringBuilder("SIG:")
        val stepX = (width / 4).coerceAtLeast(1)
        val stepY = (height / 4).coerceAtLeast(1)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                sb.append(brightness.toString(16)).append("-")
            }
        }
        sb.append(abs(baseHash))
        return sb.toString()
    }

    /**
     * Membandingkan dua signature wajah dan mengembalikan persentase kemiripan (0.0 - 1.0)
     */
    fun calculateSimilarity(sig1: String, sig2: String): Double {
        if (sig1 == sig2 && sig1.isNotBlank()) return 0.98

        if (sig1.isBlank() || sig2.isBlank()) return 0.0

        val parts1 = sig1.split("-")
        val parts2 = sig2.split("-")

        if (parts1.size < 4 || parts2.size < 4) {
            return if (sig1.hashCode() == sig2.hashCode()) 0.95 else 0.40
        }

        var totalDiff = 0.0
        val count = minOf(parts1.size, parts2.size)
        var evaluated = 0

        for (i in 0 until count) {
            val v1 = parts1[i].toIntOrNull(16)
            val v2 = parts2[i].toIntOrNull(16)
            if (v1 != null && v2 != null) {
                totalDiff += abs(v1 - v2)
                evaluated++
            }
        }

        if (evaluated == 0) return 0.50

        val avgDiff = totalDiff / evaluated
        val similarity = (1.0 - (avgDiff / 255.0)).coerceIn(0.0, 1.0)
        return similarity
    }
}
