package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppError
import com.example.data.model.GameType
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainViewModelErrorTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MainViewModel(app)
    }

    @Test
    fun testEmptyRoomCodeJoinEmitsError() {
        assertNull(viewModel.appError.value)
        viewModel.joinRoom("")
        val err = viewModel.appError.value
        assertNotNull(err)
        assertTrue(err is AppError.CustomError)

        viewModel.clearAppError()
        assertNull(viewModel.appError.value)
    }

    @Test
    fun testInvalidSecretLengthEmitsError() {
        assertNull(viewModel.appError.value)
        viewModel.selectGameType(GameType.CODE_SECRET)
        viewModel.setCodeLength(4)
        
        viewModel.setMySecretNumberAndStart("12") // length 2 when 4 expected
        val err = viewModel.appError.value
        assertNotNull(err)
        assertTrue(err is AppError.InvalidSecretInput)
        
        viewModel.clearAppError()
        assertNull(viewModel.appError.value)
    }

    @Test
    fun testRepeatedDigitsWhenNotAllowedEmitsError() {
        assertNull(viewModel.appError.value)
        viewModel.selectGameType(GameType.CODE_SECRET)
        viewModel.setCodeLength(4)
        
        viewModel.setMySecretNumberAndStart("1123") // repeated digits
        val err = viewModel.appError.value
        assertNotNull(err)
        assertTrue(err is AppError.InvalidSecretInput)
        
        viewModel.clearAppError()
        assertNull(viewModel.appError.value)
    }

    @Test
    fun testSetCustomError() {
        assertNull(viewModel.appError.value)
        viewModel.setError(AppError.NetworkConnectionFailed())
        val err = viewModel.appError.value
        assertNotNull(err)
        assertTrue(err is AppError.NetworkConnectionFailed)
        assertEquals("فشل الاتصال بالسيرفر. يرجى التحقق من اتصال الإنترنت والإعادة.", err?.messageAr)
    }
}
