package com.mugarutv.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mugarutv.data.local.PortalSessionStore
import com.mugarutv.data.model.PortalUserSession
import com.mugarutv.data.repository.PortalRepository
import com.mugarutv.data.repository.SCHOOL_APPS_SCRIPT_ENDPOINT
import com.mugarutv.ui.HUB_LOGIN_REWARD_COOLDOWN_KEY
import com.mugarutv.ui.HUB_REGISTER_REWARD_COOLDOWN_KEY
import com.mugarutv.ui.PORTAL_REWARD_COOLDOWN_MS
import com.mugarutv.ui.PORTAL_REWARD_PREFERENCE_NAME
import com.mugarutv.ui.REWARD_AD_UNIT_ID
import com.mugarutv.ui.rememberRewardedSubmissionGate
import com.mugarutv.R
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    openLoginOnStart: Boolean = false,
    onLoginSuccess: () -> Unit = {},
    onOpenRegistration: () -> Unit,
    onOpenChannelUpload: () -> Unit = onOpenRegistration
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val portalSessionStore = remember { PortalSessionStore(context) }
    val portalRepository = remember { PortalRepository() }
    val portalSession by portalSessionStore.sessionFlow.collectAsState(initial = null)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val authErrors = remember { mutableStateMapOf<String, String>() }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginMessage by remember { mutableStateOf<String?>(null) }
    var loginMessageIsError by remember { mutableStateOf(false) }
    var isSubmittingPortalAuth by remember { mutableStateOf(false) }
    var showHubLoginDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCredentialDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var credentialMessage by remember { mutableStateOf<String?>(null) }

    val isHubActive = portalSession?.status.equals("ACTIVE", ignoreCase = true)

    val hubLoginRewardGate = rememberRewardedSubmissionGate(context, REWARD_AD_UNIT_ID, PORTAL_REWARD_PREFERENCE_NAME, HUB_LOGIN_REWARD_COOLDOWN_KEY, PORTAL_REWARD_COOLDOWN_MS)

    LaunchedEffect(openLoginOnStart, isHubActive) {
        if (openLoginOnStart && !isHubActive) showHubLoginDialog = true
        else if (openLoginOnStart && isHubActive) onLoginSuccess()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SchoolBackground()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    AboutHeader(onHeaderClick = {})
                }

                item {
                    SectionHeaderTitle("Panduan Pengajuan")
                    Spacer(Modifier.height(12.dp))
                    GuidelineItem(Icons.Default.CheckCircle, "Konten Sekolah", "Materi yang dikirim berkaitan dengan kegiatan, pembelajaran, atau prestasi SMA Pembangunan Jaya 2 Sidoarjo.")
                    GuidelineItem(Icons.Default.Lock, "Hak Cipta", "Tidak melanggar hak cipta musik, video, atau materi pihak ketiga.")
                    GuidelineItem(Icons.Default.Done, "Izin Tayang", "Memberikan izin penayangan di SMA PJ2 TV.")
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Ruang digital untuk karya siswa, kegiatan sekolah, dan kabar dari Yayasan Pendidikan Jaya.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHubLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingPortalAuth) {
                    showHubLoginDialog = false
                    authErrors.clear()
                    loginMessage = null
                    loginMessageIsError = false
                }
            },
            title = { Text("Masuk Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Masuk memakai username dan password admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            authErrors.remove("login_username")
                        },
                        label = { Text("Username") },
                        isError = authErrors["login_username"] != null,
                        singleLine = true,
                        supportingText = {
                            authErrors["login_username"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            authErrors.remove("login_password")
                        },
                        label = { Text("Password") },
                        isError = authErrors["login_password"] != null,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = {
                            authErrors["login_password"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    loginMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (loginMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmittingPortalAuth,
                    onClick = {
                        authErrors.clear()
                        loginMessage = null
                    loginMessageIsError = false
                        
                        isSubmittingPortalAuth = true
                        scope.launch {
                            portalRepository.loginAdmin(username.trim(), password).onSuccess { session ->
                                portalSessionStore.saveSession(session)
                                loginMessage = "Login berhasil."
                                loginMessageIsError = false
                                showHubLoginDialog = false
                                username = ""
                                password = ""
                                authErrors.clear()
                                credentialMessage = null
                                onLoginSuccess()
                            }.onFailure { error ->
                                loginMessage = error.message ?: "Gagal login."
                                loginMessageIsError = true
                            }
                            isSubmittingPortalAuth = false
                        }
                    }
                ) {
                    Text(if (isSubmittingPortalAuth) "Masuk..." else "Masuk")
                }
            },
            dismissButton = {
                    TextButton(
                        enabled = !isSubmittingPortalAuth,
                        onClick = {
                            showHubLoginDialog = false
                            authErrors.clear()
                            loginMessage = null
                    loginMessageIsError = false
                            username = ""
                            password = ""
                        }
                    ) {
                        Text("Batal")
                    }
            }
        )
    }

    
    if (showCredentialDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingPortalAuth) {
                    showCredentialDialog = false
                    credentialMessage = null
                }
            },
            title = { Text("Edit Akun Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = {
                            newUsername = it
                            credentialMessage = null
                        },
                        label = { Text("New Username") },
                        singleLine = true,
                        enabled = !isSubmittingPortalAuth
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            credentialMessage = null
                        },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isSubmittingPortalAuth
                    )
                    credentialMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmittingPortalAuth,
                    onClick = {
                        val session = portalSession
                        if (session == null || !isHubActive) {
                            credentialMessage = "Login admin diperlukan."
                            return@Button
                        }
                        

                        isSubmittingPortalAuth = true
                        scope.launch {
                            portalRepository.updateAdminCredentials(
                                currentUsername = session.username,
                                newUsername = newUsername.trim(),
                                newPassword = newPassword
                            ).onSuccess { updatedSession ->
                                portalSessionStore.saveSession(updatedSession)
                                showCredentialDialog = false
                                credentialMessage = null
                            }.onFailure { error ->
                                credentialMessage = error.message ?: "Gagal update akun admin."
                            }
                            isSubmittingPortalAuth = false
                        }
                    }
                ) {
                    Text(if (isSubmittingPortalAuth) "Menyimpan..." else "Submit")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSubmittingPortalAuth,
                    onClick = {
                        showCredentialDialog = false
                        credentialMessage = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingPortalAuth) {
                    showLogoutConfirmDialog = false
                }
            },
            title = { Text("Keluar Akun") },
            text = { Text("Yakin mau keluar dari akun admin?") },
            confirmButton = {
                Button(
                    enabled = !isSubmittingPortalAuth,
                    onClick = {
                        portalSessionStore.clearSession()
                        showLogoutConfirmDialog = false
                        loginMessage = null
                    loginMessageIsError = false
                        authErrors.clear()
                        username = ""
                        password = ""
                    }
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSubmittingPortalAuth,
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AdminPanel(
    isHubActive: Boolean,
    username: String,
    onLoginClick: () -> Unit,
    onUploadVideoClick: () -> Unit,
    onUploadChannelClick: () -> Unit,
    onEditCredentialClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Panel Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isHubActive) "Login sebagai $username" else "Masuk untuk upload dan kelola konten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isHubActive) {
                Button(
                    onClick = onUploadVideoClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Video")
                }
                Button(
                    onClick = onUploadChannelClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Channel")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEditCredentialClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Akun")
                    }
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Keluar")
                    }
                }
            } else {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Masuk Admin")
                }
            }
        }
    }
}
@Composable
private fun AboutHeader(onHeaderClick: () -> Unit = {}) {
    var tapCount by remember { mutableStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(90.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .clickable {
                    tapCount++
                    if (tapCount >= 7) {
                        tapCount = 0
                        onHeaderClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://spj.sch.id/uploads/frontend/images/logo9.png",
                contentDescription = "Logo SMA Pembangunan Jaya 2 Sidoarjo",
                placeholder = painterResource(R.drawable.school_tv_mark),
                error = painterResource(R.drawable.school_tv_mark),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Tentang Kami",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "SMA PJ2 TV menyajikan kegiatan, karya, dan prestasi SMA Pembangunan Jaya 2 Sidoarjo di bawah Yayasan Pendidikan Jaya.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuidelineItem(icon: ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeaderTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}






