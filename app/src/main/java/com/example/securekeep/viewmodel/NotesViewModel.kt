package com.example.securekeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.securekeep.data.SecurityManager
import com.example.securekeep.data.local.Note
import com.example.securekeep.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NotesViewModel(
    private val repository: NotesRepository,
    private val securityManager: SecurityManager,
) : ViewModel() {
    val allNotes: Flow<List<Note>> = repository.activeNotes
    val deletedNotes: Flow<List<Note>> = repository.deletedNotes

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked = _isAppUnlocked.asStateFlow()

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    val appPin: StateFlow<String?> = securityManager.appPin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "LOADING_PIN"
    )

    val notePin: StateFlow<String?> = securityManager.notePin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val useBiometricApp: StateFlow<Boolean> = securityManager.useBiometricApp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val useBiometricNote: StateFlow<Boolean> = securityManager.useBiometricNote.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isDarkMode: StateFlow<Boolean?> = securityManager.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        cleanupOldNotes()
    }

    private fun cleanupOldNotes() = viewModelScope.launch(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        repository.cleanupOldNotes(thirtyDaysAgo)
    }


    suspend fun insert(note: Note): Int {
        val id = withContext(Dispatchers.IO) {
            repository.insert(note).toInt()
        }
        val updatedNote = if (note.id == 0) note.copy(id = id) else note

        return id
    }

    fun moveToTrash(note: Note) = viewModelScope.launch {
        val trashedNote = note.copy(isDeleted = true, deletedTimestamp = System.currentTimeMillis())
        insert(trashedNote)
    }

    fun restoreNote(note: Note) = viewModelScope.launch {
        val restoredNote = note.copy(isDeleted = false, deletedTimestamp = null)
        insert(restoredNote)
    }

    fun deletePermanently(note: Note) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.deletePermanently(note)
        }
    }

    fun emptyTrash() = viewModelScope.launch {
        // This is a bit more complex for full sync, but we can iterate through deleted notes
        withContext(Dispatchers.IO) {
            // Note: repository.emptyTrash() should be called, but we also need to delete from Firestore
            // A simple implementation:
            repository.emptyTrash()
            // We'd need a list of IDs to delete from Firestore here
        }
    }

    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }

    fun saveAppPin(pin: String) = viewModelScope.launch {
        securityManager.saveAppPin(pin)
    }

    fun saveNotePin(pin: String) = viewModelScope.launch {
        securityManager.saveNotePin(pin)
    }

    fun setUseBiometricApp(enabled: Boolean) = viewModelScope.launch {
        securityManager.setUseBiometricApp(enabled)
    }

    fun setUseBiometricNote(enabled: Boolean) = viewModelScope.launch {
        securityManager.setUseBiometricNote(enabled)
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        securityManager.setDarkMode(enabled)
    }

    fun clearNotePin() = viewModelScope.launch {
        securityManager.clearNotePin()
    }

    fun toggleNoteLock(note: Note) = viewModelScope.launch {
        insert(note.copy(isLocked = !note.isLocked))
    }
}

class NotesViewModelFactory(
    private val repository: NotesRepository,
    private val securityManager: SecurityManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(
                repository,
                securityManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
