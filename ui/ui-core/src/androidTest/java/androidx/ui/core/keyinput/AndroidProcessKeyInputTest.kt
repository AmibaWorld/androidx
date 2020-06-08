/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.core.keyinput

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.KEYCODE_A
import android.view.KeyEvent.ACTION_UP
import android.view.ViewGroup
import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.OwnerAmbient
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.setFocusableContent
import androidx.ui.core.keyinput.Key.Companion.A
import androidx.ui.core.keyinput.KeyEventType.KeyDown
import androidx.ui.core.keyinput.KeyEventType.KeyUp
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test verifies that an Android key event triggers a Compose key event. More detailed test
 * cases are present at [ProcessKeyInputTest].
 */
@SmallTest
@RunWith(Parameterized::class)
class AndroidProcessKeyInputTest(val keyEventAction: Int) {
    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "keyEventAction = {0}")
        fun initParameters() = listOf(ACTION_UP, ACTION_DOWN)
    }

    @Test
    fun onKeyEvent_triggered() {
        // Arrange.
        lateinit var ownerView: ViewGroup
        lateinit var focusModifier: FocusModifier
        lateinit var receivedKeyEvent: KeyEvent
        composeTestRule.setFocusableContent {
            ownerView = getOwner()
            focusModifier = FocusModifier()
            Box(
                modifier = focusModifier.keyInputFilter {
                    receivedKeyEvent = it
                    true
                }
            )
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = runOnIdleCompose {
            ownerView.dispatchKeyEvent(AndroidKeyEvent(keyEventAction, KEYCODE_A))
        }

        // Assert.
        runOnIdleCompose {
            val keyEventType = when (keyEventAction) {
                ACTION_UP -> KeyUp
                ACTION_DOWN -> KeyDown
                else -> error("No tests for this key action.")
            }
            assertThat(receivedKeyEvent).isEqualTo(KeyEvent(A, keyEventType))
            assertThat(keyConsumed).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    private fun getOwner() = OwnerAmbient.current as ViewGroup
}