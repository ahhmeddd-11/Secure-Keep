package com.example.securekeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.securekeep.drive.DriveSyncRepository
import com.example.securekeep.repository.NotesRepository

class DriveSyncViewModelFactory(
    private val syncRepository: DriveSyncRepository,
    private val notesRepository: NotesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriveSyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DriveSyncViewModel(syncRepository, notesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
