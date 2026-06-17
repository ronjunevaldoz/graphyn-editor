package com.ronjunevaldoz.graphyn

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 * TODO sample only for now
 *
 *  Placeholder for now
 *  screenshot location at /app/shared/build/output/roborazzi
 */
class MainKmpTest {
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test() = runDesktopComposeUiTest {
    setContent {
      DemoApp()
    }
    val roborazziOptions = RoborazziOptions(
      recordOptions = RoborazziOptions.RecordOptions(
        resizeScale = 0.5
      ),
      compareOptions = RoborazziOptions.CompareOptions(
        changeThreshold = 0F
      )
    )
    onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
  }
}