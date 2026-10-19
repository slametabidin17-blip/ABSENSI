package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.Member
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QRCodeUtil {

    /**
     * Menghasilkan Bitmap QR Code dari string konten dengan level koreksi kesalahan tinggi.
     */
    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Membuat string payload QR standar untuk anggota presensi.
     */
    fun getMemberQrPayload(member: Member): String {
        return if (member.identifier.isNotBlank()) member.identifier else member.id
    }

    /**
     * Mencari anggota yang cocok dari hasil pemindaian teks QR code.
     */
    fun parseMemberFromQr(rawQrText: String, members: List<Member>): Member? {
        val clean = rawQrText.trim()
        if (clean.isBlank()) return null

        // 1. Coba pencocokan exact identifier (NIS / NIP)
        members.firstOrNull { it.identifier.equals(clean, ignoreCase = true) }?.let { return it }

        // 2. Coba pencocokan exact id dokumen
        members.firstOrNull { it.id == clean }?.let { return it }

        // 3. Coba format ber-prefix (misal: "SF:20240101", "NIS:20240101", "ID:xyz")
        val stripped = clean.substringAfter(":").trim()
        if (stripped.isNotBlank() && stripped != clean) {
            members.firstOrNull {
                it.identifier.equals(stripped, ignoreCase = true) || it.id == stripped
            }?.let { return it }
        }

        // 4. Cek apakah ada nomor identitas yang terkandung di dalam teks QR
        for (member in members) {
            if (member.identifier.isNotBlank() && clean.contains(member.identifier, ignoreCase = true)) {
                return member
            }
        }

        return null
    }
}
