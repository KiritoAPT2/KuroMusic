package com.kuromusic.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.kuromusic",
            profileBlock = {
                startActivityAndWait()
                device.waitForIdle()

                android.util.Log.d("BaselineProfile", "=== MODO MANUAL ===")
                android.util.Log.d("BaselineProfile", "Esperando 5 minutos para interacción manual...")
                android.util.Log.d("BaselineProfile", "Sigue las instrucciones en pantalla.")

                // Wait 1 minute for manual interaction
                val startTime = System.currentTimeMillis()
                val timeoutMs = 60_000L // 1 minute
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    device.waitForIdle()
                    Thread.sleep(5_000) // check every 5 seconds
                }

                android.util.Log.d("BaselineProfile", "=== FIN MODO MANUAL ===")
            },
        )
    }
}
