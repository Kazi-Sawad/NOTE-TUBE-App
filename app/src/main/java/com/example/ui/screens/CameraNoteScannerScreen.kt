package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteLanguage
import com.example.data.remote.CameraAnalysisResult
import com.example.ui.viewmodel.CameraScanUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraNoteScannerScreen(
    scanState: CameraScanUiState,
    selectedLanguage: NoteLanguage,
    onLanguageSelected: (NoteLanguage) -> Unit,
    onScanImage: (Bitmap, NoteLanguage, String) -> Unit,
    onSaveToLibrary: (CameraAnalysisResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userHint by remember { mutableStateOf("") }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    // Camera capture launcher (returns full-size or preview bitmap)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    // Gallery picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                }
                capturedBitmap = bitmap
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load selected image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Camera Note Vision",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "AI Handwritten & Exam Note Scanner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Description Banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan Handwritten Notes & Textbooks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Snap a picture of your physical notes, whiteboard, or textbook equations. NoteTube Vision transcribes, summarizes, and extracts key exam points.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector Pill
            ExposedDropdownMenuBox(
                expanded = languageDropdownExpanded,
                onExpandedChange = { languageDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${selectedLanguage.flag} ${selectedLanguage.displayName} (${selectedLanguage.nativeName})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Output Note Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                ExposedDropdownMenu(
                    expanded = languageDropdownExpanded,
                    onDismissRequest = { languageDropdownExpanded = false }
                ) {
                    NoteLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text("${lang.flag} ${lang.displayName} — ${lang.nativeName}")
                            },
                            onClick = {
                                onLanguageSelected(lang)
                                languageDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Context/Hint input
            OutlinedTextField(
                value = userHint,
                onValueChange = { userHint = it },
                label = { Text("Optional Topic Hint or Specific Questions") },
                placeholder = { Text("e.g., Physics kinematics, Chapter 4 exam review, Calculus limits") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Camera / Image preview container
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (capturedBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Note Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            IconButton(onClick = { capturedBitmap = null }) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Retake",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Document Captured Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Take a picture of handwritten notes, whiteboard formulas, or textbook pages to generate an instant synthesis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Capture from Camera & Pick from Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { takePictureLauncher.launch(null) },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("take_photo_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.weight(1f).testTag("pick_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("From Gallery", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analyze Action Button
            val isScanning = scanState is CameraScanUiState.Scanning
            Button(
                onClick = {
                    val bmp = capturedBitmap
                    if (bmp != null) {
                        onScanImage(bmp, selectedLanguage, userHint)
                    } else {
                        Toast.makeText(context, "Please take or select a photo of your notes first", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = capturedBitmap != null && !isScanning,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("analyze_note_image_btn")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = (scanState as CameraScanUiState.Scanning).message,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyze & Summarize Note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Error Display
            if (scanState is CameraScanUiState.Error) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Results Display
            if (scanState is CameraScanUiState.Success) {
                val result = scanState.result
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = result.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Button(
                                onClick = { onSaveToLibrary(result) },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Library", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "EXECUTIVE SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = result.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Text(
                            text = "TRANSCRIBED TEXT & FORMULAS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = result.extractedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (result.keyPoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "KEY CONCEPTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            result.keyPoints.forEach { point ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (result.examQuestionsOrFormulas.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "EXAM PREP & CRITICAL FORMULAS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                            result.examQuestionsOrFormulas.forEach { q ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = q,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
