package com.mugarutv.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mugarutv.data.model.SongRegistrationPayload
import com.mugarutv.data.repository.SongRegistrationRepository
import com.mugarutv.ui.SelectedImagePayload
import com.mugarutv.ui.readSelectedImage
import kotlinx.coroutines.launch

enum class UploadFormMode {
    Video,
    Channel
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitSongScreen(
    modifier: Modifier = Modifier,
    mode: UploadFormMode = UploadFormMode.Video,
    onBack: (() -> Unit)? = null,
    onSubmitted: () -> Unit = {},
    repository: SongRegistrationRepository = remember { SongRegistrationRepository() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    val pageTitle = if (mode == UploadFormMode.Channel) "Upload Channel" else "Upload Video"

    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("https://www.youtube.com/@smapembangunanjaya2sidoarj824") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCoverImage by remember { mutableStateOf<SelectedImagePayload?>(null) }

    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedImage(context, uri)
                .onSuccess { selectedCoverImage = it }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(error.message ?: "Gagal memuat cover art.")
                }
        }
    }

    fun validateVideo(): Boolean {
        fieldErrors.clear()
        if (youtubeUrl.isBlank()) fieldErrors["youtubeUrl"] = "Link video wajib diisi"
        return fieldErrors.isEmpty()
    }

    fun validateChannel(): Boolean {
        fieldErrors.clear()
        if (channelUrl.isBlank()) fieldErrors["channelUrl"] = "Link Channel YouTube wajib diisi"
        return fieldErrors.isEmpty()
    }

    fun clearVideoForm() {
        title = ""
        year = ""
        synopsis = ""
        youtubeUrl = ""
        selectedCoverImage = null
        fieldErrors.clear()
    }

    fun clearChannelForm() {
        channelUrl = "https://www.youtube.com/@smapembangunanjaya2sidoarj824"
        fieldErrors.clear()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (onBack != null) {
                item {
                    TopAppBar(
                        title = {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (mode == UploadFormMode.Channel) {
                item {
                    FormField(
                        value = channelUrl,
                        onValueChange = {
                            channelUrl = it
                            fieldErrors.remove("channelUrl")
                        },
                        label = "Link Channel YouTube",
                        error = fieldErrors["channelUrl"]
                    )
                }
                item {
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            if (!validateChannel()) {
                                scope.launch { snackbarHostState.showSnackbar("Link Channel YouTube wajib diisi.") }
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val result = repository.importYoutubeChannel(channelUrl.trim())
                                isLoading = false

                                result.onSuccess { importResult ->
                                    snackbarHostState.showSnackbar(
                                        "Upload channel selesai: ${importResult.imported} baru, ${importResult.skipped} sudah ada."
                                    )
                                    clearChannelForm()
                                    onSubmitted()
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar("Gagal upload channel: ${error.message ?: "Terjadi kesalahan."}")
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                item {
                    FormField(
                        value = title,
                        onValueChange = {
                            title = it
                            fieldErrors.remove("title")
                        },
                        label = "Judul",
                        error = fieldErrors["title"]
                    )
                }
                item {
                    FormField(
                        value = year,
                        onValueChange = {
                            year = it
                            fieldErrors.remove("year")
                        },
                        label = "Tahun",
                        error = fieldErrors["year"],
                        keyboardType = KeyboardType.Number
                    )
                }
                item {
                    FormField(
                        value = synopsis,
                        onValueChange = {
                            synopsis = it
                            fieldErrors.remove("synopsis")
                        },
                        label = "Deskripsi",
                        error = fieldErrors["synopsis"],
                        minLines = 4
                    )
                }
                item {
                    FormField(
                        value = youtubeUrl,
                        onValueChange = {
                            youtubeUrl = it
                            fieldErrors.remove("youtubeUrl")
                        },
                        label = "Link Video",
                        error = fieldErrors["youtubeUrl"]
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cover Art (Opsional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Kalau ada gambar cover, pilih manual. Kalau tidak, aplikasi pakai tampilan fallback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(if (selectedCoverImage == null) "Pilih Cover Art" else "Ganti Cover Art")
                        }
                        selectedCoverImage?.let { cover ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    bitmap = cover.previewBitmap.asImageBitmap(),
                                    contentDescription = "Preview cover art",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(2.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = cover.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = cover.mimeType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            if (!validateVideo()) {
                                scope.launch { snackbarHostState.showSnackbar("Link video wajib diisi.") }
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val payload = SongRegistrationPayload(
                                    title = title.trim(),
                                    synopsis = synopsis.trim(),
                                    year = year.trim(),
                                    kategori = "YouTube",
                                    youtubeUrl = youtubeUrl.trim(),
                                    copyrightAgreement = true,
                                    coverImageBase64 = selectedCoverImage?.base64,
                                    coverImageFileName = selectedCoverImage?.fileName,
                                    coverImageMimeType = selectedCoverImage?.mimeType
                                )

                                val result = repository.submitRegistration(payload)
                                isLoading = false

                                result.onSuccess {
                                    snackbarHostState.showSnackbar("Upload video berhasil dikirim.")
                                    clearVideoForm()
                                    onSubmitted()
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar("Gagal upload video: ${error.message ?: "Terjadi kesalahan."}")
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(
            imeAction = if (minLines == 1) ImeAction.Done else ImeAction.Default,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus(force = true) }
        ),
        supportingText = {
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
