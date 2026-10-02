package com.example.musicroom.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.service.DelegatedDevice
import com.example.musicroom.data.service.DeviceApiService
import com.example.musicroom.data.service.MyDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceControlUiState(
    val isLoading: Boolean = false,
    val myDevices: List<MyDevice> = emptyList(),
    val delegatedToMe: List<DelegatedDevice> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val deviceApiService: DeviceApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceControlUiState())
    val uiState: StateFlow<DeviceControlUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val myDevices = deviceApiService.getMyDevices().getOrElse { emptyList() }
            val delegatedToMe = deviceApiService.getDelegatedToMe().getOrElse { emptyList() }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                myDevices = myDevices,
                delegatedToMe = delegatedToMe
            )
        }
    }

    fun delegateControl(deviceId: String, friendEmail: String) {
        viewModelScope.launch {
            deviceApiService.delegateControl(deviceId, friendEmail)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(message = message)
                    loadData()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    fun revokeControl(deviceId: String, userId: Int) {
        viewModelScope.launch {
            deviceApiService.revokeControl(deviceId, userId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(message = message)
                    loadData()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    fun sendCommand(deviceId: String, action: String) {
        viewModelScope.launch {
            deviceApiService.sendCommand(deviceId, action)
                .onSuccess { message -> _uiState.value = _uiState.value.copy(message = message) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.message) }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}
