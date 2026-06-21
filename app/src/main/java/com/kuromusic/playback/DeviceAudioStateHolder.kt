package com.kuromusic.playback

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

enum class OutputDevice {
    HEADPHONES,
    SPEAKER,
    BLUETOOTH,
    UNKNOWN,
}

class DeviceAudioStateHolder(private val context: Context) {

    @Volatile
    var device: OutputDevice = OutputDevice.UNKNOWN
        private set

    fun detect(): OutputDevice {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val btAdapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: SecurityException) {
            null
        }

        val isBtConnected = try {
            btAdapter?.isEnabled == true &&
                btAdapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
        } catch (_: SecurityException) {
            false
        }

        if (isBtConnected) {
            device = OutputDevice.BLUETOOTH
            return device
        }

        val isWired = audioManager?.let { mgr ->
            mgr.getDevices(AudioManager.GET_DEVICES_ALL).any { info ->
                info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                info.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } ?: false

        if (isWired) {
            device = OutputDevice.HEADPHONES
            return device
        }

        device = OutputDevice.SPEAKER
        return device
    }

    fun refresh() {
        detect()
    }
}
