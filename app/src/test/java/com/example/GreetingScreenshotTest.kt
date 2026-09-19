package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.VideoNote
import com.example.ui.screens.NoteCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleNote = VideoNote(
      id = 1,
      videoUrl = "https://www.youtube.com/watch?v=kGgAhy_e1n8",
      videoId = "kGgAhy_e1n8",
      title = "Mastering Focus and Deep Work in a Distracted World",
      channel = "Productivity Hub",
      thumbnailUrl = "",
      category = "Productivity",
      summary = "A comprehensive exploration of cognitive science behind intense concentration.",
      keyTakeaways = "[\"Insight 1\", \"Insight 2\"]",
      structuredBullets = "[]",
      actionItems = "[\"Action 1\"]",
      userNotes = "",
      isFavorite = true,
      createdAt = 1700000000000L
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        NoteCard(
          note = sampleNote,
          onClick = {},
          onToggleFavorite = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
