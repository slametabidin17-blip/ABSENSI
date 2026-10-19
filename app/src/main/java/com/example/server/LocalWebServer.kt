package com.example.server

import android.content.Context
import android.util.Log
import com.example.model.AttendanceRecord
import com.example.model.Member
import com.example.model.SchoolSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class LocalWebServer(
    private val port: Int = 8080,
    private val dataProvider: DataProvider
) {
    interface DataProvider {
        fun getSchoolSchedule(): SchoolSchedule
        fun getMembers(): List<Member>
        fun getTodayAttendances(): List<AttendanceRecord>
        fun getAllAttendances(): List<AttendanceRecord>
        fun onAddMemberFromWeb(member: Member)
        fun onAddAttendanceFromWeb(record: AttendanceRecord)
        fun onUpdateScheduleFromWeb(schedule: SchoolSchedule)
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.i("LocalWebServer", "Web Server started on port $port")

                while (isActive && isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalWebServer", "Failed to start server on port $port", e)
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverJob?.cancel()
        serverSocket = null
        Log.i("LocalWebServer", "Web Server stopped")
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val output = s.getOutputStream()

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val path = parts[1]

                // Parse headers
                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    val lower = line.lowercase()
                    if (lower.startsWith("content-length:")) {
                        contentLength = lower.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                // Handle CORS preflight
                if (method == "OPTIONS") {
                    sendOptionsResponse(output)
                    return
                }

                // Read request body if present
                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val count = reader.read(buffer, read, contentLength - read)
                        if (count == -1) break
                        read += count
                    }
                    String(buffer, 0, read)
                } else ""

                when {
                    method == "GET" && (path == "/" || path == "/index.html") -> serveDashboardHtml(output)
                    method == "GET" && path == "/api/attendances" -> serveAttendancesJson(output)
                    method == "POST" && path == "/api/attendances" -> handlePostAttendance(output, body)
                    method == "GET" && path == "/api/members" -> serveMembersJson(output)
                    method == "POST" && path == "/api/members" -> handlePostMember(output, body)
                    method == "GET" && path == "/api/stats" -> serveStatsJson(output)
                    method == "POST" && path == "/api/settings" -> handlePostSettings(output, body)
                    method == "GET" && path == "/api/export" -> serveExportCsv(output)
                    method == "GET" && path.startsWith("/api/qr") -> serveQrImage(output, path)
                    else -> serve404(output)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalWebServer", "Error handling HTTP client request", e)
        }
    }

    private fun serveQrImage(output: OutputStream, path: String) {
        try {
            val query = path.substringAfter("?", "")
            val raw = query.split("&")
                .firstOrNull { it.startsWith("data=") }
                ?.substringAfter("data=")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "SMARTFACE"

            val bitmap = com.example.util.QRCodeUtil.generateQrCodeBitmap(raw, size = 320)
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()

            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/png\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(headers.toByteArray(StandardCharsets.UTF_8))
            output.write(bytes)
            output.flush()
        } catch (e: Exception) {
            serve404(output)
        }
    }

    private fun serveDashboardHtml(output: OutputStream) {
        val webFile = File("/web/index.html")
        val htmlContent = if (webFile.exists()) {
            webFile.readText(StandardCharsets.UTF_8)
        } else {
            generateFallbackHtml()
        }
        sendResponse(output, 200, "OK", "text/html; charset=UTF-8", htmlContent.toByteArray(StandardCharsets.UTF_8))
    }

    private fun handlePostAttendance(output: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val memberId = json.optString("memberId", "")
            val memberName = json.optString("memberName", "")
            val memberRole = json.optString("memberRole", "STUDENT")
            val departmentOrClass = json.optString("departmentOrClass", "")
            val status = json.optString("status", "ON_TIME")
            val date = json.optString("date", "")
            val type = json.optString("type", "CHECK_IN")
            val verificationMethod = json.optString("verificationMethod", "MANUAL_WEB_PORTAL")

            val record = AttendanceRecord(
                memberId = memberId,
                memberName = memberName,
                memberRole = memberRole,
                departmentOrClass = departmentOrClass,
                status = status,
                date = date,
                time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                type = type,
                verificationMethod = verificationMethod,
                confidence = 1.0
            )
            dataProvider.onAddAttendanceFromWeb(record)

            val resp = """{"status":"ok","message":"Attendance recorded"}"""
            sendResponse(output, 200, "OK", "application/json; charset=UTF-8", resp.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            val err = """{"status":"error","message":"${e.message}"}"""
            sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", err.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun handlePostMember(output: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val name = json.optString("name", "")
            val role = json.optString("role", "STUDENT")
            val identifier = json.optString("identifier", "")
            val departmentOrClass = json.optString("departmentOrClass", "")

            val member = Member(
                name = name,
                role = role,
                identifier = identifier,
                departmentOrClass = departmentOrClass,
                faceFeatureData = "WEB_REG_${name.hashCode()}"
            )
            dataProvider.onAddMemberFromWeb(member)

            val resp = """{"status":"ok","message":"Member created"}"""
            sendResponse(output, 200, "OK", "application/json; charset=UTF-8", resp.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            val err = """{"status":"error","message":"${e.message}"}"""
            sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", err.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun handlePostSettings(output: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val current = dataProvider.getSchoolSchedule()
            val updated = current.copy(
                schoolName = json.optString("schoolName", current.schoolName),
                checkInDeadline = json.optString("checkInDeadline", current.checkInDeadline),
                checkOutStart = json.optString("checkOutStart", current.checkOutStart)
            )
            dataProvider.onUpdateScheduleFromWeb(updated)

            val resp = """{"status":"ok","message":"Settings updated"}"""
            sendResponse(output, 200, "OK", "application/json; charset=UTF-8", resp.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            val err = """{"status":"error","message":"${e.message}"}"""
            sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", err.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun serveAttendancesJson(output: OutputStream) {
        val records = dataProvider.getTodayAttendances()
        val jsonArray = records.joinToString(separator = ",", prefix = "[", postfix = "]") { r ->
            """{"id":"${escapeJson(r.id)}","memberId":"${escapeJson(r.memberId)}","memberName":"${escapeJson(r.memberName)}","memberRole":"${r.memberRole}","departmentOrClass":"${escapeJson(r.departmentOrClass)}","date":"${r.date}","time":"${r.time}","type":"${r.type}","status":"${r.status}","verificationMethod":"${r.verificationMethod}"}"""
        }
        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", jsonArray.toByteArray(StandardCharsets.UTF_8))
    }

    private fun serveMembersJson(output: OutputStream) {
        val members = dataProvider.getMembers()
        val jsonArray = members.joinToString(separator = ",", prefix = "[", postfix = "]") { m ->
            """{"id":"${escapeJson(m.id)}","name":"${escapeJson(m.name)}","role":"${m.role}","identifier":"${escapeJson(m.identifier)}","departmentOrClass":"${escapeJson(m.departmentOrClass)}"}"""
        }
        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", jsonArray.toByteArray(StandardCharsets.UTF_8))
    }

    private fun serveStatsJson(output: OutputStream) {
        val records = dataProvider.getTodayAttendances()
        val total = records.size
        val onTime = records.count { it.status == "ON_TIME" }
        val late = records.count { it.status == "LATE" }
        val teachers = records.count { it.memberRole == "TEACHER" || it.memberRole == "STAFF" }
        val students = records.count { it.memberRole == "STUDENT" }

        val json = """{"total":$total,"onTime":$onTime,"late":$late,"teachers":$teachers,"students":$students}"""
        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", json.toByteArray(StandardCharsets.UTF_8))
    }

    private fun serveExportCsv(output: OutputStream) {
        val records = dataProvider.getAllAttendances()
        val csv = StringBuilder("ID,Tanggal,Waktu,Nama Lengkap,Peran,Kelas/Bagian,Tipe,Status,Metode Verifikasi\n")
        for (r in records) {
            csv.append("\"${r.id}\",\"${r.date}\",\"${r.time}\",\"${r.memberName}\",\"${r.memberRole}\",\"${r.departmentOrClass}\",\"${r.type}\",\"${r.status}\",\"${r.verificationMethod}\"\n")
        }
        val bytes = csv.toString().toByteArray(StandardCharsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/csv; charset=UTF-8\r\n" +
                "Content-Disposition: attachment; filename=\"rekap_presensi.csv\"\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(headers.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendOptionsResponse(output: OutputStream) {
        val headers = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    private fun serve404(output: OutputStream) {
        val msg = "Halaman Tidak Ditemukan"
        sendResponse(output, 404, "Not Found", "text/plain; charset=UTF-8", msg.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendResponse(output: OutputStream, code: Int, status: String, contentType: String, body: ByteArray) {
        val headers = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(StandardCharsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "")
    }

    private fun generateFallbackHtml(): String {
        return "<html><body><h1>SmartFace Web Reporting</h1><p>Memuat sistem pelaporan...</p></body></html>"
    }
}
