package com.example.mindarc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PushUpCounterState(
    val count: Int = 0,
    val formFeedback: String = "Position yourself in front of the camera",
    val isDetecting: Boolean = false,
    val lastElbowAngle: Float? = null
)

class PushUpCounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(PushUpCounterState())
    val state: StateFlow<PushUpCounterState> = _state.asStateFlow()
    
    private var wasDown = false // Track if we were in the down position
    
    fun updateElbowAngle(angle: Float?) {
        val currentState = _state.value
        _state.value = currentState.copy(
            lastElbowAngle = angle,
            isDetecting = angle != null
        )
        
        angle?.let { currentAngle ->
            val feedback = when {
                currentAngle < 90 -> {
                    wasDown = true
                    "Go Lower"
                }
                currentAngle > 160 && wasDown -> {
                    wasDown = false
                    incrementCount()
                    "Great Rep!"
                }
                currentAngle > 160 -> {
                    wasDown = false
                    "Good Position"
                }
                else -> {
                    "Keep Going"
                }
            }
            
            _state.value = _state.value.copy(formFeedback = feedback)
        } ?: run {
            _state.value = _state.value.copy(
                formFeedback = "Position yourself in front of the camera",
                isDetecting = false
            )
        }
    }
    
    private fun incrementCount() {
        _state.value = _state.value.copy(
            count = _state.value.count + 1
        )
    }
    
    fun resetCount() {
        _state.value = PushUpCounterState()
        wasDown = false
    }
    
    fun getCurrentCount(): Int = _state.value.count
}

