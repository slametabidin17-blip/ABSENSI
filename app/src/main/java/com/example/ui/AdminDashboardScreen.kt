package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRepository
import com.example.model.AttendanceRecord
import com.example.model.Member
import com.example.model.SchoolSchedule
import com.example.ui.theme.AmberLate
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.util.NetworkUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    repository: AttendanceRepository,
    currentUserId: String,
    onBackToKiosk: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle system back navigation
    BackHandler {
        onBackToKiosk()
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var schedule by remember { mutableStateOf(SchoolSchedule(schoolName = "SMP Negeri 1")) }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var attendances by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var showEnrollDialog by remember { mutableStateOf(false) }
    var qrDialogMember by remember { mutableStateOf<Member?>(null) }

    val networkInfo = remember { NetworkUtil.getLocalIpAddress() }
    val webServerUrl = "http://${networkInfo.ipAddress}:8080"

    // Realtime listeners
    LaunchedEffect(currentUserId) {
        launch {
            repository.observeSchedule(currentUserId).collect { schedule = it }
        }
        launch {
            repository.observeMembers(currentUserId).collect { members = it }
        }
        launch {
            repository.observeAttendances(currentUserId).collect { attendances = it }
        }
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBackToKiosk,
                                modifier = Modifier.testTag("back_to_kiosk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Kiosk Presensi",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Portal Admin & Pelaporan",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = schedule.schoolName.ifBlank { "SMP Negeri 1" },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar Akun",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Navigation Bar
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = TealDark,
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Ringkasan") },
                            icon = { Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_overview")
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Rekap Presensi") },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_attendances")
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Anggota & QR") },
                            icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_members")
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text("Pengaturan") },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 2) {
                FloatingActionButton(
                    onClick = { showEnrollDialog = true },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_member_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Anggota")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SlateBackground)
        ) {
            when (selectedTabIndex) {
                0 -> OverviewTab(
                    schedule = schedule,
                    members = members,
                    attendances = attendances,
                    webServerUrl = webServerUrl,
                    onCopyUrl = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("URL Web Server", webServerUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "URL disalin ke papan klip: $webServerUrl", Toast.LENGTH_SHORT).show()
                    }
                )
                1 -> AttendanceLogsTab(
                    attendances = attendances,
                    onExportCsv = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$webServerUrl/api/export"))
                        context.startActivity(intent)
                    }
                )
                2 -> MembersTab(
                    members = members,
                    onDeleteMember = { memberId ->
                        coroutineScope.launch {
                            repository.deleteMember(currentUserId, memberId)
                            Toast.makeText(context, "Anggota dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShowQr = { member ->
                        qrDialogMember = member
                    }
                )
                3 -> SettingsTab(
                    schedule = schedule,
                    webServerUrl = webServerUrl,
                    onSaveSchedule = { updated ->
                        coroutineScope.launch {
                            repository.saveSchedule(currentUserId, updated)
                            Toast.makeText(context, "Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSeedSample = {
                        coroutineScope.launch {
                            repository.seedSampleDataIfEmpty(currentUserId)
                            Toast.makeText(context, "Data sampel guru, siswa, dan QR berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Dialog Pendaftaran Wajah Anggota Baru
            if (showEnrollDialog) {
                EnrollMemberDialog(
                    onDismiss = { showEnrollDialog = false },
                    onSaveMember = { newMember ->
                        coroutineScope.launch {
                            repository.addMember(currentUserId, newMember)
                            showEnrollDialog = false
                            Toast.makeText(context, "Anggota ${newMember.name} berhasil terdaftar", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Dialog Kartu QR Code Anggota
            qrDialogMember?.let { member ->
                MemberQrCardDialog(
                    member = member,
                    schedule = schedule,
                    onDismiss = { qrDialogMember = null }
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    schedule: SchoolSchedule,
    members: List<Member>,
    attendances: List<AttendanceRecord>,
    webServerUrl: String,
    onCopyUrl: () -> Unit
) {
    val context = LocalContext.current
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayAttendances = attendances.filter { it.date == todayStr }

    val totalToday = todayAttendances.size
    val onTimeCount = todayAttendances.count { it.status == "ON_TIME" }
    val lateCount = todayAttendances.count { it.status == "LATE" }
    val teachersCount = todayAttendances.count { it.memberRole == "TEACHER" || it.memberRole == "STAFF" }
    val studentsCount = todayAttendances.count { it.memberRole == "STUDENT" }

    val faceVerifiedCount = todayAttendances.count { it.verificationMethod.contains("FACE", ignoreCase = true) }
    val qrVerifiedCount = todayAttendances.count { it.verificationMethod.contains("QR", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ethernet Web Server Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lan,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Server Web Pelaporan LAN",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Status: Aktif & Melayani via Port 8080",
                                    fontSize = 12.sp,
                                    color = GreenSuccess
                                )
                            }
                        }

                        IconButton(onClick = onCopyUrl) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin URL",
                                tint = TealPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Buka alamat ini pada browser komputer/laptop TU di jaringan Ethernet yang sama:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = webServerUrl,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webServerUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_web_dashboard_button")
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Web Pelaporan di Browser")
                    }
                }
            }
        }

        // Stats KPI Grid
        item {
            Text(
                text = "Statistik Kehadiran Hari Ini",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Hadir",
                    value = "$totalToday",
                    subtitle = "Dari ${members.size} terdaftar",
                    accentColor = TealPrimary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Tepat Waktu",
                    value = "$onTimeCount",
                    subtitle = "Sebelum ${schedule.checkInDeadline}",
                    accentColor = GreenSuccess
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Terlambat",
                    value = "$lateCount",
                    subtitle = "Setelah ${schedule.checkInDeadline}",
                    accentColor = AmberLate
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Guru & Staf",
                    value = "$teachersCount",
                    subtitle = "$studentsCount Siswa Hadir",
                    accentColor = CyanAccent
                )
            }
        }

        // Dual Verification Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Metode Verifikasi Kehadiran",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Wajah AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                                }
                                Text("$faceVerifiedCount Siswa/Guru", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                                Text("Pengenalan Wajah Utama", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("QR Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyanAccent)
                                }
                                Text("$qrVerifiedCount Siswa/Guru", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                                Text("Pindai Kartu Instan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AttendanceLogsTab(
    attendances: List<AttendanceRecord>,
    onExportCsv: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rekapitulasi Presensi (${attendances.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekspor CSV")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari berdasarkan nama atau kelas...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        val filtered = attendances.filter {
            it.memberName.contains(searchQuery, ignoreCase = true) ||
                    it.departmentOrClass.contains(searchQuery, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Belum ada rekaman presensi",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.memberName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${record.memberRole} • ${record.departmentOrClass}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "${record.date} • ${record.time}",
                                        fontSize = 11.sp,
                                        color = TealPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isQr = record.verificationMethod.contains("QR", ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isQr) CyanAccent.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isQr) Icons.Default.QrCodeScanner else Icons.Default.Face,
                                                contentDescription = null,
                                                tint = if (isQr) CyanAccent else TealPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isQr) "QR Code" else "Wajah AI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isQr) CyanAccent else TealPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (record.status == "ON_TIME") GreenSuccess.copy(alpha = 0.15f)
                                        else AmberLate.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (record.status == "ON_TIME") "Tepat Waktu" else "Terlambat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.status == "ON_TIME") GreenSuccess else AmberLate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    members: List<Member>,
    onDeleteMember: (String) -> Unit,
    onShowQr: (Member) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Daftar Guru, Staf & Siswa (${members.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (members.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Belum ada anggota terdaftar. Ketuk '+' untuk menambah.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (member.role == "TEACHER") Color(0xFF3B82F6) else TealLight
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${member.identifier} • ${member.departmentOrClass}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            // Tombol Lihat Kartu QR
                            IconButton(
                                onClick = { onShowQr(member) },
                                modifier = Modifier.testTag("show_qr_${member.identifier}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Lihat Kartu QR",
                                    tint = TealPrimary
                                )
                            }

                            // Tombol Hapus Anggota
                            IconButton(onClick = { onDeleteMember(member.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    schedule: SchoolSchedule,
    webServerUrl: String,
    onSaveSchedule: (SchoolSchedule) -> Unit,
    onSeedSample: () -> Unit
) {
    var schoolName by remember(schedule) { mutableStateOf(schedule.schoolName) }
    var checkInStart by remember(schedule) { mutableStateOf(schedule.checkInStart) }
    var checkInDeadline by remember(schedule) { mutableStateOf(schedule.checkInDeadline) }
    var checkOutStart by remember(schedule) { mutableStateOf(schedule.checkOutStart) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Pengaturan Jam Sekolah",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("Nama Sekolah / Madrasah") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = checkInStart,
                            onValueChange = { checkInStart = it },
                            label = { Text("Mulai Masuk") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkInDeadline,
                            onValueChange = { checkInDeadline = it },
                            label = { Text("Batas Terlambat") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = checkOutStart,
                        onValueChange = { checkOutStart = it },
                        label = { Text("Mulai Jam Pulang") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val updated = schedule.copy(
                                schoolName = schoolName,
                                checkInStart = checkInStart,
                                checkInDeadline = checkInDeadline,
                                checkOutStart = checkOutStart
                            )
                            onSaveSchedule(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Text("Simpan Pengaturan")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Data Demonstrasi & QR Code Siswa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Tambahkan data sampel siswa & guru lengkap dengan foto dan nomor kartu QR siap scan untuk keperluan pengujian sistem.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = onSeedSample,
                        colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("seed_sample_button")
                    ) {
                        Text("Muat Data Sampel Guru & Siswa", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
