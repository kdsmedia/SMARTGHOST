package com.altomedia.smartghost

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Runtime smoke test: the real Application class is instantiated on the JVM.
 * If Hilt wiring or the manifest were broken, the app would not start at all.
 * This is the closest we can get to "the app launches" without a device.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = SmartGhostApp::class)
class AppBootsRuntimeTest {

    @Test
    fun application_starts_and_is_the_real_class() {
        val app = ApplicationProvider.getApplicationContext<SmartGhostApp>()
        assertNotNull(app)
        assertEquals("com.altomedia.smartghost", app.packageName)
        assertEquals(Build.VERSION_CODES.TIRAMISU, Build.VERSION.SDK_INT)
    }
}
