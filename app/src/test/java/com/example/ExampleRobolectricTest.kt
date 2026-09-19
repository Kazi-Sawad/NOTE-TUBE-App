package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.YouTubeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NoteTube", appName)
  }

  @Test
  fun `extract video ID from standard YouTube URL`() {
    val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val videoId = YouTubeHelper.extractVideoId(url)
    assertEquals("dQw4w9WgXcQ", videoId)
  }

  @Test
  fun `extract video ID from short YouTube URL`() {
    val url = "https://youtu.be/dQw4w9WgXcQ"
    val videoId = YouTubeHelper.extractVideoId(url)
    assertEquals("dQw4w9WgXcQ", videoId)
  }

  @Test
  fun `extract video ID from shorts URL`() {
    val url = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
    val videoId = YouTubeHelper.extractVideoId(url)
    assertEquals("dQw4w9WgXcQ", videoId)
  }
}
