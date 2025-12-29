package com.example.mindarc.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MindArcViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MindArcViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(ReadingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReadingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

