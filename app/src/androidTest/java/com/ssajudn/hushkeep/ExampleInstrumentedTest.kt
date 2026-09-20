package com.ssajudn.hushkeep

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.ssajudn.hushkeep.feature.auth.OtpVerificationScreen
import com.ssajudn.hushkeep.ui.theme.HushkeepTheme

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ssajudn.hushkeep", appContext.packageName)
    }

    @Test
    fun otpPreparationScreenRendersWithoutActivatingProductionFlow() {
        composeRule.setContent {
            HushkeepTheme {
                OtpVerificationScreen(
                    email = "test@example.com",
                    isLoading = false,
                    errorMessage = null,
                    onVerify = {},
                    onResend = {},
                )
            }
        }

        composeRule.onNodeWithText("Verifikasi email").assertIsDisplayed()
        composeRule.onNodeWithText("Masukkan kode 6 digit yang dikirim ke test@example.com.")
            .assertIsDisplayed()
    }
}
