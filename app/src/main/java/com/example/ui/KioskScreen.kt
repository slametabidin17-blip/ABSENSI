package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraXAttendanceAnalyzer
import com.example.camera.FaceBoundingBoxOverlay
import com.example.camera.ScannerMode
import com.example.data.AttendanceRepository
import com.example.model.AttendanceRecord
import com.example.model.Member
import com.example.model.SchoolSchedule
import com.example.ui.theme.AmberLate
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.SlateDarkBackground
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.util.NetworkUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun KioskScreen(
    repository: AttendanceRepository,
    currentUserId: String,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var schedule by remember { mutableStateOf(SchoolSchedule(schoolName = "SMP Negeri 1")) }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var recentAttendances by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }
    var attendanceType by remember { mutableStateOf("CHECK_IN") } // CHECK_IN or CHECK_OUT

    var verifiedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Live Camera, Face & QR Scanner states
    var scannerMode by remember { mutableStateOf(ScannerMode.DUAL) }
    var detectedFaces by remember { mutableStateOf<List<CameraXAttendanceAnalyzer.FaceData>>(emptyList()) }
    var detectedQrs by remember { mutableStateOf<List<CameraXAttendanceAnalyzer.QrData>>(emptyList()) }
    var frameWidth by remember { mutableIntStateOf(0) }
    var frameHeight by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }

    // Member QR Card modal preview
    var selectedMemberForQr by remember { mutableStateOf<Member?>(null) }
    var showMemberPickerForQr by remember { mutableStateOf(false) }

    // Track face recognition retry counter to proactively suggest QR code secondary check-in
    var faceUnmatchedTicks by remember { mutableIntStateOf(0) }
    var showQrFallbackSuggestion by remember { mutableStateOf(false) }

    // Ethernet network info
    var networkInfo by remember { mutableStateOf(NetworkUtil.getLocalIpAddress()) }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Refresh network IP periodically
    LaunchedEffect(Unit) {
        while (true) {
            networkInfo = NetworkUtil.getLocalIpAddress()
            delay(10000)
        }
    }

    // Digital clock ticker
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        while (true) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            delay(1000)
        }
    }

    // Watch face detection to suggest QR Code fallback if face is struggling to match
    LaunchedEffect(detectedFaces) {
        if (detectedFaces.isNotEmpty() && detectedFaces.all { it.matchedMember == null || it.matchConfidence < 0.85 }) {
            faceUnmatchedTicks++
            if (faceUnmatchedTicks >= 4) {
                showQrFallbackSuggestion = true
            }
        } else {
            faceUnmatchedTicks = 0
            if (detectedFaces.isEmpty()) {
                showQrFallbackSuggestion = false
            }
        }
    }

    // Observe schedule, members, and attendances from repository
    LaunchedEffect(currentUserId) {
        launch {
            repository.observeSchedule(currentUserId).collect {
                schedule = it
            }
        }
        launch {
            repository.observeMembers(currentUserId).collect {
                members = it
            }
        }
        launch {
            repository.observeAttendances(currentUserId).collect {
                recentAttendances = it.take(5)
            }
        }
    }

    // Auto-dismiss verified attendance dialog
    LaunchedEffect(verifiedRecord) {
        if (verifiedRecord != null) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            } catch (e: Exception) {
                // ignore
            }

            delay(2800)
            verifiedRecord = null
            showQrFallbackSuggestion = false
        }
    }

    // Root Kiosk Surface
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateDarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                KioskHeader(
                    schoolName = schedule.schoolName.ifBlank { "SMP Negeri 1" },
                    currentTime = currentTimeStr,
                    currentDate = currentDateStr,
                    networkInfo = networkInfo,
                    onOpenAdmin = { showPinDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Selector Bar: Dual vs Face vs QR Code
                ScannerModeSelector(
                    currentMode = scannerMode,
                    onModeSelected = {
                        scannerMode = it
                        showQrFallbackSuggestion = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Center Viewport: CameraX + Dual Face & QR Code Analyzer Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        // CameraX Feed with Dual Face + QR Code Analyzer
                        CameraXView(
                            enrolledMembers = { members },
                            scannerMode = { scannerMode },
                            useFrontCamera = useFrontCamera,
                            onDetections = { faces, qrs, width, height ->
                                detectedFaces = faces
                                detectedQrs = qrs
                                frameWidth = width
                                frameHeight = height
                            },
                            onFaceVerified = { verifiedMember, confidence ->
                                coroutineScope.launch {
                                    val record = repository.recordAttendance(
                                        userId = currentUserId,
                                        member = verifiedMember,
                                        type = attendanceType,
                                        verificationMethod = "FACE_RECOGNITION",
                                        confidence = confidence
                                    )
                                    verifiedRecord = record
                                }
                            },
                            onQrVerified = { verifiedMember, qrCode ->
                                coroutineScope.launch {
                                    val record = repository.recordAttendance(
                                        userId = currentUserId,
                                        member = verifiedMember,
                                        type = attendanceType,
                                        verificationMethod = "QR_CODE",
                                        confidence = 1.0
                                    )
                                    verifiedRecord = record
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live Face & QR Code Bounding Box Overlay
                        FaceBoundingBoxOverlay(
                            faces = detectedFaces,
                            qrCodes = detectedQrs,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                            isFrontCamera = useFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Izin Kamera",
                                tint = TealLight,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Kamera Siap Digunakan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.padding(top = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Text("Izinkan Akses Kamera")
                            }
                        }
                    }

                    // Futuristic Scanner Overlay (Dynamic Face Frame & QR Target)
                    AttendanceScannerOverlay(
                        scannerMode = scannerMode,
                        attendanceType = attendanceType,
                        deadline = schedule.checkInDeadline,
                        detectedFacesCount = detectedFaces.size,
                        detectedQrsCount = detectedQrs.size,
                        onSwitchCamera = { useFrontCamera = !useFrontCamera }
                    )

                    // Secondary QR Fallback Prompt if Face Recognition struggles
                    AnimatedVisibility(
                        visible = showQrFallbackSuggestion && scannerMode != ScannerMode.QR_ONLY,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(AmberLate, CyanAccent))
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wajah Sulit Dikenali?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Beralih ke Pindai QR Code untuk absen cepat",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        scannerMode = ScannerMode.QR_ONLY
                                        showQrFallbackSuggestion = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("switch_to_qr_fallback_button")
                                ) {
                                    Text("Mode QR", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Control & Quick Verification Row
                KioskBottomBar(
                    attendanceType = attendanceType,
                    onToggleType = {
                        attendanceType = if (attendanceType == "CHECK_IN") "CHECK_OUT" else "CHECK_IN"
                    },
                    members = members,
                    onOpenMemberQr = { showMemberPickerForQr = true },
                    onMemberIdentified = { member ->
                        coroutineScope.launch {
                            val record = repository.recordAttendance(
                                userId = currentUserId,
                                member = member,
                                type = attendanceType,
                                verificationMethod = if (scannerMode == ScannerMode.QR_ONLY) "QR_CODE" else "FACE_RECOGNITION",
                                confidence = 0.98
                            )
                            verifiedRecord = record
                        }
                    }
                )
            }

            // Success Overlay Banner when Face or QR Code is Recognized
            AnimatedVisibility(
                visible = verifiedRecord != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier.align(Alignment.Center)
            ) {
                verifiedRecord?.let { record ->
                    SuccessAttendanceCard(record = record)
                }
            }

            // Member QR Card Dialog Preview
            selectedMemberForQr?.let { member ->
                MemberQrCardDialog(
                    member = member,
                    schedule = schedule,
                    onDismiss = { selectedMemberForQr = null },
                    onQuickCheckIn = { m ->
                        coroutineScope.launch {
                            val record = repository.recordAttendance(
                                userId = currentUserId,
                                member = m,
                                type = attendanceType,
                                verificationMethod = "QR_CODE",
                                confidence = 1.0
                            )
                            verifiedRecord = record
                        }
                    }
                )
            }

            // Quick Picker Dialog for Member QR
            if (showMemberPickerForQr) {
                MemberPickerForQrDialog(
                    members = members,
                    onMemberSelected = { member ->
                        showMemberPickerForQr = false
                        selectedMemberForQr = member
                    },
                    onDismiss = { showMemberPickerForQr = false }
                )
            }

            // Admin PIN Verification Dialog
            if (showPinDialog) {
                AdminPinDialog(
                    pinInput = pinInput,
                    onPinChange = { pinInput = it },
                    pinError = pinError,
                    onDismiss = {
                        showPinDialog = false
                        pinInput = ""
                        pinError = null
                    },
                    onConfirm = {
                        if (pinInput == "1234" || pinInput == "admin") {
                            showPinDialog = false
                            pinInput = ""
                            pinError = null
                            onNavigateToAdmin()
                        } else {
                            pinError = "PIN salah. Coba: 1234"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScannerModeSelector(
    currentMode: ScannerMode,
    onModeSelected: (ScannerMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeTabItem(
                title = "Wajah & QR (Dual)",
                icon = Icons.Default.SyncAlt,
                isSelected = currentMode == ScannerMode.DUAL,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScannerMode.DUAL) },
                testTag = "mode_dual"
            )
            ModeTabItem(
                title = "Wajah Saja",
                icon = Icons.Default.Face,
                isSelected = currentMode == ScannerMode.FACE_ONLY,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScannerMode.FACE_ONLY) },
                testTag = "mode_face"
            )
            ModeTabItem(
                title = "QR Code Saja",
                icon = Icons.Default.QrCodeScanner,
                isSelected = currentMode == ScannerMode.QR_ONLY,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScannerMode.QR_ONLY) },
                testTag = "mode_qr"
            )
        }
    }
}

@Composable
private fun ModeTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) TealPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun KioskHeader(
    schoolName: String,
    currentTime: String,
    currentDate: String,
    networkInfo: NetworkUtil.NetworkInfo,
    onOpenAdmin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = schoolName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GreenSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ethernet Server: http://${networkInfo.ipAddress}:8080",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TealLight
                    )
                }
            }

            // Right side: Clock & Admin Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentTime.ifBlank { "--:--:--" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = currentDate,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                IconButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.2f))
                        .testTag("admin_portal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Menu Admin",
                        tint = TealLight
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceScannerOverlay(
    scannerMode: ScannerMode,
    attendanceType: String,
    deadline: String,
    detectedFacesCount: Int,
    detectedQrsCount: Int,
    onSwitchCamera: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Frame Guide: Face Oval or QR Square
        if (scannerMode == ScannerMode.QR_ONLY) {
            // Kotak Target Khusus QR Code
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.verticalGradient(listOf(CyanAccent, GreenSuccess, CyanAccent)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Scanning Laser Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .padding(top = (laserPosition * 220).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GreenSuccess, Color.White, GreenSuccess, Color.Transparent)
                            )
                        )
                )

                // Corner indicators
                Text(
                    text = "Arahkan QR Code ke Kotak Ini",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        } else {
            // Oval Face Guide Frame (Dual or Face Only)
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 300.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(CyanAccent, TealLight, CyanAccent)
                        ),
                        shape = RoundedCornerShape(110.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Animated Scanning Laser Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(2.5.dp)
                        .padding(top = (laserPosition * 260).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, CyanAccent, Color.White, CyanAccent, Color.Transparent)
                            )
                        )
                )
            }
        }

        // Top Status Pill & Camera switch button
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(9999.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            detectedQrsCount > 0 -> Icons.Default.QrCodeScanner
                            detectedFacesCount > 0 -> Icons.Default.Face
                            scannerMode == ScannerMode.QR_ONLY -> Icons.Default.QrCode
                            else -> Icons.Default.Face
                        }
                        val tint = when {
                            detectedQrsCount > 0 -> GreenSuccess
                            detectedFacesCount > 0 -> GreenSuccess
                            else -> CyanAccent
                        }
                        val text = when {
                            detectedQrsCount > 0 -> "$detectedQrsCount QR Code Terdeteksi"
                            detectedFacesCount > 0 -> "$detectedFacesCount Wajah Terdeteksi"
                            scannerMode == ScannerMode.QR_ONLY -> "Menunggu Kartu/QR Code..."
                            scannerMode == ScannerMode.FACE_ONLY -> "Mencari Wajah..."
                            else -> "Siap Pindai Wajah & QR..."
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Camera Switch Button
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Ganti Kamera Depan/Belakang",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = if (attendanceType == "CHECK_IN") "Presensi Masuk (Batas: $deadline)" else "Presensi Pulang",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun KioskBottomBar(
    attendanceType: String,
    onToggleType: () -> Unit,
    members: List<Member>,
    onOpenMemberQr: () -> Unit,
    onMemberIdentified: (Member) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleType,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (attendanceType == "CHECK_IN") TealPrimary else Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("toggle_attendance_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (attendanceType == "CHECK_IN") "Absen Masuk" else "Absen Pulang")
                }

                // Tombol Lihat Kartu QR Siswa / Guru
                Button(
                    onClick = onOpenMemberQr,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.testTag("open_my_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kartu QR Saya", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Scan Member Chips
            if (members.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        Card(
                            modifier = Modifier
                                .clickable { onMemberIdentified(member) }
                                .testTag("quick_scan_${member.identifier}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (member.role == "TEACHER") Color(0xFF3B82F6).copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (member.role == "TEACHER") Color(0xFF3B82F6) else TealLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(1).uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = member.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = member.departmentOrClass,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Belum ada anggota terdaftar. Buka Menu Admin untuk mendaftarkan Guru & Siswa.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SuccessAttendanceCard(record: AttendanceRecord) {
    val isQr = record.verificationMethod.contains("QR", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (record.status == "ON_TIME") GreenSuccess else AmberLate),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isQr) Icons.Default.QrCodeScanner else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isQr) "PRESENSI QR CODE BERHASIL!" else "PRESENSI WAJAH BERHASIL!",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (record.status == "ON_TIME") GreenSuccess else AmberLate
            )

            Text(
                text = record.memberName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "${record.memberRole} • ${record.departmentOrClass}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Method Badge
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isQr) GreenSuccess.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isQr) Icons.Default.QrCode else Icons.Default.Face,
                        contentDescription = null,
                        tint = if (isQr) GreenSuccess else CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isQr) "Verifikasi QR Code (Akurat & Cepat)" else "Verifikasi Pengenalan Wajah AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isQr) GreenSuccess else CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAKTU", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(record.time, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STATUS", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        text = if (record.status == "ON_TIME") "TEPAT WAKTU" else "TERLAMBAT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.status == "ON_TIME") GreenSuccess else AmberLate
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIPE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        text = if (record.type == "CHECK_IN") "MASUK" else "PULANG",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberPickerForQrDialog(
    members: List<Member>,
    onMemberSelected: (Member) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Kartu QR Anggota")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pilih nama untuk menampilkan kartu QR digital yang dapat dipindai pada kamera:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama atau NIS...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                val filtered = members.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.identifier.contains(searchQuery, ignoreCase = true) ||
                            it.departmentOrClass.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "Tidak ada anggota yang cocok",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtered.take(6).forEach { member ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMemberSelected(member) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${member.role} • ${member.departmentOrClass} • ${member.identifier}", fontSize = 11.sp)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun AdminPinDialog(
    pinInput: String,
    onPinChange: (String) -> Unit,
    pinError: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Akses Portal Admin")
            }
        },
        text = {
            Column {
                Text(
                    text = "Masukkan PIN administrator untuk membuka pelaporan dan pengaturan (PIN Default: 1234)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinChange,
                    label = { Text("PIN Admin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                )
                if (pinError != null) {
                    Text(
                        text = pinError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_pin_button")
            ) {
                Text("Masuk Admin")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun CameraXView(
    enrolledMembers: () -> List<Member>,
    scannerMode: () -> ScannerMode,
    useFrontCamera: Boolean,
    onDetections: (faces: List<CameraXAttendanceAnalyzer.FaceData>, qrs: List<CameraXAttendanceAnalyzer.QrData>, width: Int, height: Int) -> Unit,
    onFaceVerified: (member: Member, confidence: Double) -> Unit,
    onQrVerified: (member: Member, qrCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    val analyzer = CameraXAttendanceAnalyzer(
                        enrolledMembers = enrolledMembers,
                        scannerMode = scannerMode,
                        onDetections = onDetections,
                        onFaceVerified = onFaceVerified,
                        onQrVerified = onQrVerified
                    )
                    imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    } catch (e2: Exception) {
                        // ignore fallback
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    val analyzer = CameraXAttendanceAnalyzer(
                        enrolledMembers = enrolledMembers,
                        scannerMode = scannerMode,
                        onDetections = onDetections,
                        onFaceVerified = onFaceVerified,
                        onQrVerified = onQrVerified
                    )
                    imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}
