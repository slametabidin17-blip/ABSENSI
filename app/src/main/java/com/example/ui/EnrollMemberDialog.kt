package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Member
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TealPrimary
import com.example.util.FaceMatchingUtil

@Composable
fun EnrollMemberDialog(
    onDismiss: () -> Unit,
    onSaveMember: (Member) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") } // TEACHER, STAFF, STUDENT
    var identifier by remember { mutableStateOf("") } // NIP/NIS
    var departmentOrClass by remember { mutableStateOf("") }
    var isFaceScanned by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pendaftaran Anggota & Wajah",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daftarkan Guru, Staf, atau Siswa untuk absensi",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Role selection chips
                Text(
                    text = "Peran / Kategori",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = role == "STUDENT",
                        onClick = { role = "STUDENT" },
                        label = { Text("Siswa") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = TealPrimary
                        )
                    )
                    FilterChip(
                        selected = role == "TEACHER",
                        onClick = { role = "TEACHER" },
                        label = { Text("Guru") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = TealPrimary
                        )
                    )
                    FilterChip(
                        selected = role == "STAFF",
                        onClick = { role = "STAFF" },
                        label = { Text("Staf / TU") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = TealPrimary
                        )
                    )
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (role == "STUDENT") "Nama Lengkap Siswa" else "Nama Lengkap & Gelar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Identifier (NIS / NIP)
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(if (role == "STUDENT") "Nomor Induk Siswa (NIS)" else "NIP / Nomor Pegawai") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Department or Class
                OutlinedTextField(
                    value = departmentOrClass,
                    onValueChange = { departmentOrClass = it },
                    label = { Text(if (role == "STUDENT") "Kelas (Contoh: Kelas 7A, 8B)" else "Mata Pelajaran / Bagian Tugas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_dept_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Biometric Face Capture Card
                Text(
                    text = "Perekaman Data Biometrik Wajah",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable {
                            // Simulate taking face photo / landmark enrollment
                            isFaceScanned = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFaceScanned) GreenSuccess.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (isFaceScanned) GreenSuccess else TealPrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isFaceScanned) GreenSuccess else TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFaceScanned) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                contentDescription = "Ambil Foto Wajah",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFaceScanned) "Data Wajah Berhasil Dipindai" else "Ambil / Daftarkan Foto Wajah",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFaceScanned) GreenSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFaceScanned) "Fitur biometrik tersimpan untuk pencocokan otomatis" else "Sentuh di sini untuk memindai wajah melalui kamera",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || identifier.isBlank() || departmentOrClass.isBlank()) {
                                validationError = "Mohon lengkapi semua kolom isian"
                                return@Button
                            }
                            isSaving = true
                            val faceSig = FaceMatchingUtil.generateFaceSignature(name, identifier, null)
                            val newMember = Member(
                                name = name.trim(),
                                role = role,
                                identifier = identifier.trim(),
                                departmentOrClass = departmentOrClass.trim(),
                                faceFeatureData = faceSig
                            )
                            onSaveMember(newMember)
                        },
                        modifier = Modifier.testTag("save_member_button"),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan Anggota")
                        }
                    }
                }
            }
        }
    }
}
