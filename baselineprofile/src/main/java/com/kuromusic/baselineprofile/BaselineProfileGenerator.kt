package com.kuromusic.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
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
                device.wait(Until.hasObject(By.descContains("Home")), 10_000)

                val listView = device.findObject(By.depth(10))
                listView?.let {
                    it.setGestureMargin(device.displayWidth / 5)
                    it.fling(Direction.DOWN)
                    it.fling(Direction.UP)
                }

                device.waitForIdle()
            },
        )
    }
}
