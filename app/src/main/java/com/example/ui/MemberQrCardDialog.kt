package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Member
import com.example.model.SchoolSchedule
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.util.QRCodeUtil

@Composable
fun MemberQrCardDialog(
    member: Member,
    schedule: SchoolSchedule,
    onDismiss: () -> Unit,
    onQuickCheckIn: ((Member) -> Unit)? = null
) {
    val qrPayload = remember(member) {
        QRCodeUtil.getMemberQrPayload(member)
    }

    val qrBitmap = remember(qrPayload) {
        try {
            QRCodeUtil.generateQrCodeBitmap(qrPayload, size = 512)
        } catch (e: Exception) {
            null
        }
    }

    val isTeacher = member.role == "TEACHER" || member.role == "STAFF"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("member_qr_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TealPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = TealLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = schedule.schoolName.ifBlank { "SMP Negeri 1" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isTeacher) "KARTU IDENTITAS GURU & STAF" else "KARTU PELAJAR & PRESENSI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qr_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Member Info Card Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTeacher) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF064E3B).copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(TealLight, CyanAccent)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isTeacher) Color(0xFF3B82F6) else TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = member.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${if (isTeacher) "NIP/ID" else "NIS"}: ${member.identifier.ifBlank { member.id.take(8) }}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${member.role} • ${member.departmentOrClass}",
                                fontSize = 11.sp,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Container
                Card(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code ${member.name}",
                                modifier = Modifier
                                    .size(196.dp)
                                    .testTag("qr_code_image")
                            )
                        } else {
                            Text(
                                text = "Gagal memuat QR Code",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payload text & instructions
                Text(
                    text = "KODE QR: $qrPayload",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TealLight
                )

                Text(
                    text = "Arahkan kode QR ini ke kamera absensi kiosk untuk presensi cepat otomatis sebagai cadangan jika wajah terhalang.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("close_qr_modal"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Tutup", color = Color.White)
                    }

                    if (onQuickCheckIn != null) {
                        Button(
                            onClick = {
                                onQuickCheckIn(member)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("test_scan_qr_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulasi Scan", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
