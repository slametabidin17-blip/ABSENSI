package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.AttendanceRepository
import com.example.server.LocalWebServer
import com.example.ui.AdminDashboardScreen
import com.example.ui.AuthScreen
import com.example.ui.KioskScreen
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private var localWebServer: LocalWebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AttendanceRepository(applicationContext)

        // Start background Local Web Server serving Ethernet port 8080
        localWebServer = LocalWebServer(port = 8080, dataProvider = repository).also {
            it.start()
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainContent(repository = repository)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localWebServer?.stop()
        localWebServer = null
    }
}

@Composable
fun AppMainContent(repository: AttendanceRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var currentScreen by remember { mutableStateOf("KIOSK") } // "KIOSK" or "ADMIN"

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val userId = currentUser!!.uid

        // Seed sample school data if this is the first run
        LaunchedEffect(userId) {
            repository.seedSampleDataIfEmpty(userId)
        }

        when (currentScreen) {
            "KIOSK" -> {
                KioskScreen(
                    repository = repository,
                    currentUserId = userId,
                    onNavigateToAdmin = {
                        currentScreen = "ADMIN"
                    }
                )
            }
            "ADMIN" -> {
                AdminDashboardScreen(
                    repository = repository,
                    currentUserId = userId,
                    onBackToKiosk = {
                        currentScreen = "KIOSK"
                    },
                    onSignOut = {
                        signOut(
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                currentUser = null
                                currentScreen = "KIOSK"
                            },
                            scope = scope
                        )
                    }
                )
            }
        }
    }
}
