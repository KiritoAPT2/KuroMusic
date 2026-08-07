package com.kuromusic.eq

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.eq.data.EQProfileRepository
import com.kuromusic.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EQViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService
) : ViewModel() {

    private val _state = MutableStateFlow(EQState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            eqProfileRepository.profiles.collect { profiles ->
                _state.value = _state.value.copy(
                    profiles = profiles,
                    activeProfileId = eqProfileRepository.getActiveProfile()?.id
                )
            }
        }
    }

    fun selectProfile(profileId: String?) {
        viewModelScope.launch {
            if (profileId == null) {
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
            } else {
                val profile = eqProfileRepository.getAllProfiles().find { it.id == profileId }
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    if (result.isSuccess) {
                        eqProfileRepository.setActiveProfile(profileId)
                    } else {
                        _state.value = _state.value.copy(
                            error = result.exceptionOrNull()?.message
                        )
                    }
                }
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            eqProfileRepository.deleteProfile(profileId)
        }
    }

    fun importCustomProfile(
        fileName: String,
        inputStream: java.io.InputStream,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val content = inputStream.bufferedReader().readText()
                val parametricEQ = com.kuromusic.eq.data.ParametricEQParser.parseText(content)
                eqProfileRepository.importCustomProfile(fileName, parametricEQ)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
