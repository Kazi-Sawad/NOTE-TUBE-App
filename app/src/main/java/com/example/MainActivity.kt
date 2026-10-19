package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraNoteScannerScreen
import com.example.ui.screens.GenerateNoteSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.OfficialNotesLibraryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GenerationUiState
import com.example.ui.viewmodel.NoteTubeViewModel
import kotlinx.coroutines.launch

enum class AppActiveScreen {
    HOME,
    CAMERA_SCANNER,
    OFFICIAL_LIBRARY,
    NOTE_DETAIL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NoteTubeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTubeApp(viewModel: NoteTubeViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedNoteMode by viewModel.selectedNoteMode.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val cameraScanState by viewModel.cameraScanState.collectAsStateWithLifecycle()
    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()
    val officialNotes by viewModel.officialNotes.collectAsStateWithLifecycle()

    var activeScreen by remember { mutableStateOf(AppActiveScreen.HOME) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    // Synchronize detail note navigation
    LaunchedEffect(currentNote) {
        if (currentNote != null) {
            activeScreen = AppActiveScreen.NOTE_DETAIL
        } else if (activeScreen == AppActiveScreen.NOTE_DETAIL) {
            activeScreen = AppActiveScreen.HOME
        }
    }

    // Auto-dismiss sheet when note generation succeeds
    LaunchedEffect(generationState) {
        if (generationState is GenerationUiState.Success) {
            sheetState.hide()
            isSheetOpen = false
            viewModel.clearGenerationState()
            activeScreen = AppActiveScreen.NOTE_DETAIL
        }
    }

    // System Back Navigation handling
    BackHandler(enabled = activeScreen != AppActiveScreen.HOME) {
        if (activeScreen == AppActiveScreen.NOTE_DETAIL) {
            viewModel.selectNote(null)
        }
        activeScreen = AppActiveScreen.HOME
    }

    AnimatedContent(
        targetState = activeScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "NoteTubeAppScreenTransition"
    ) { screen ->
        when (screen) {
            AppActiveScreen.HOME -> {
                HomeScreen(
                    notes = notes,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                    onCategoryChanged = { viewModel.onCategorySelected(it) },
                    onNoteClick = {
                        viewModel.selectNote(it)
                        activeScreen = AppActiveScreen.NOTE_DETAIL
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenGenerateSheet = { isSheetOpen = true },
                    onOpenCameraScanner = { activeScreen = AppActiveScreen.CAMERA_SCANNER },
                    onOpenOfficialLibrary = { activeScreen = AppActiveScreen.OFFICIAL_LIBRARY },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppActiveScreen.CAMERA_SCANNER -> {
                CameraNoteScannerScreen(
                    scanState = cameraScanState,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onScanImage = { bitmap, lang, hint ->
                        viewModel.analyzeCameraImage(bitmap, lang, hint)
                    },
                    onSaveToLibrary = { result ->
                        viewModel.saveCameraAnalysisAsNote(result)
                        activeScreen = AppActiveScreen.NOTE_DETAIL
                    },
                    onBack = {
                        viewModel.clearCameraScanState()
                        activeScreen = AppActiveScreen.HOME
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppActiveScreen.OFFICIAL_LIBRARY -> {
                OfficialNotesLibraryScreen(
                    officialNotes = officialNotes,
                    onSelectNote = { officialNote ->
                        viewModel.selectOfficialNote(officialNote)
                    },
                    onSaveToLibrary = { officialNote ->
                        viewModel.saveOfficialNoteToLibrary(officialNote)
                        activeScreen = AppActiveScreen.NOTE_DETAIL
                    },
                    onBack = { activeScreen = AppActiveScreen.HOME },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppActiveScreen.NOTE_DETAIL -> {
                val note = currentNote
                if (note != null) {
                    NoteDetailScreen(
                        note = note,
                        onBack = {
                            viewModel.selectNote(null)
                            activeScreen = AppActiveScreen.HOME
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteNote = {
                            viewModel.deleteNote(it)
                            activeScreen = AppActiveScreen.HOME
                        },
                        onUpdateUserNotes = { id, text -> viewModel.updateUserNotes(id, text) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    activeScreen = AppActiveScreen.HOME
                }
            }
        }
    }

    if (isSheetOpen) {
        GenerateNoteSheet(
            sheetState = sheetState,
            generationState = generationState,
            selectedLanguage = selectedLanguage,
            selectedMode = selectedNoteMode,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onModeSelected = { viewModel.setNoteMode(it) },
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    isSheetOpen = false
                    viewModel.clearGenerationState()
                }
            },
            onGenerate = { url, context, forceOffline, language, mode ->
                viewModel.generateNotesForVideo(url, context, forceOffline, language, mode)
            }
        )
    }
}
