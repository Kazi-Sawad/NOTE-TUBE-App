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
import com.example.ui.screens.GenerateNoteSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GenerationUiState
import com.example.ui.viewmodel.NoteTubeViewModel
import kotlinx.coroutines.launch

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
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    // Auto-dismiss sheet when note generation succeeds
    LaunchedEffect(generationState) {
        if (generationState is GenerationUiState.Success) {
            sheetState.hide()
            isSheetOpen = false
            viewModel.clearGenerationState()
        }
    }

    // Handle system back navigation if inside detail screen
    BackHandler(enabled = currentNote != null) {
        viewModel.selectNote(null)
    }

    AnimatedContent(
        targetState = currentNote,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "NoteTubeScreenTransition"
    ) { activeNote ->
        if (activeNote != null) {
            NoteDetailScreen(
                note = activeNote,
                onBack = { viewModel.selectNote(null) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDeleteNote = { viewModel.deleteNote(it) },
                onUpdateUserNotes = { id, text -> viewModel.updateUserNotes(id, text) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            HomeScreen(
                notes = notes,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                onCategoryChanged = { viewModel.onCategorySelected(it) },
                onNoteClick = { viewModel.selectNote(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onOpenGenerateSheet = { isSheetOpen = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (isSheetOpen) {
        GenerateNoteSheet(
            sheetState = sheetState,
            generationState = generationState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    isSheetOpen = false
                    viewModel.clearGenerationState()
                }
            },
            onGenerate = { url, context, forceOffline ->
                viewModel.generateNotesForVideo(url, context, forceOffline)
            }
        )
    }
}
