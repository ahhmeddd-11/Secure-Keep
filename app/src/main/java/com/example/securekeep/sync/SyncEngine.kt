package com.example.securekeep.sync

import com.example.securekeep.data.local.Note
import com.example.securekeep.data.local.SyncState
import com.example.securekeep.sync.model.BackupNote

/**
 * Pure merge logic for reconciling local and remote note collections.
 * All methods are stateless and side-effect-free — suitable for testing.
 */
object SyncEngine {

    /**
     * Merges [local] and [remote] note lists using UUID-based deduplication.
     *
     * Conflict resolution rules (per note with same UUID):
     * 1. Higher [Note.version] wins.
     * 2. If versions are equal, later [Note.updatedAt] wins.
     * 3. Local notes not present in remote are preserved (they will be uploaded next backup).
     * 4. Remote notes not present locally are added (as new Room rows with id=0).
     *
     * The returned list contains exactly one entry per UUID.
     */
    fun mergeNotes(local: List<Note>, remote: List<BackupNote>): List<Note> {
        val localByUuid = local.associateBy { it.uuid }.toMutableMap()
        val result = mutableListOf<Note>()
        val seenUuids = mutableSetOf<String>()

        for (remoteNote in remote) {
            seenUuids.add(remoteNote.uuid)
            val localNote = localByUuid[remoteNote.uuid]

            if (localNote == null) {
                // New note from remote — add it with id=0 so Room auto-generates a local id
                result.add(remoteNote.toNote(id = 0, syncState = SyncState.SYNCED))
            } else {
                val winner = resolveConflict(localNote, remoteNote)
                result.add(winner)
            }
        }

        // Keep local notes that are not yet uploaded to remote
        for ((uuid, note) in localByUuid) {
            if (uuid !in seenUuids) {
                result.add(note)
            }
        }

        return result
    }

    private fun resolveConflict(local: Note, remote: BackupNote): Note {
        return when {
            remote.version > local.version ->
                remote.toNote(id = local.id, syncState = SyncState.SYNCED)
            remote.version < local.version ->
                local
            remote.updatedAt > local.updatedAt ->
                remote.toNote(id = local.id, syncState = SyncState.SYNCED)
            else ->
                local
        }
    }

    /**
     * Converts a [Note] to a [BackupNote] for JSON serialization.
     */
    fun toBackupNote(note: Note): BackupNote = BackupNote(
        uuid = note.uuid,
        title = note.title,
        content = note.content,
        color = note.color,
        isPinned = note.isPinned,
        isLocked = note.isLocked,
        lockedName = note.lockedName,
        isDeleted = note.isDeleted,
        deletedTimestamp = note.deletedTimestamp,
        imageUri = note.imageUri,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
        timestamp = note.timestamp,
        version = note.version,
        checksum = note.checksum
    )

    /**
     * Converts a [BackupNote] to a [Note] for Room insertion.
     * [id] should be 0 for new notes (Room auto-generates) or the existing local id for updates.
     */
    private fun BackupNote.toNote(id: Int, syncState: SyncState): Note = Note(
        id = id,
        uuid = uuid,
        title = title,
        content = content,
        color = color,
        isPinned = isPinned,
        isLocked = isLocked,
        lockedName = lockedName,
        isDeleted = isDeleted,
        deletedTimestamp = deletedTimestamp,
        imageUri = imageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        timestamp = timestamp,
        version = version,
        syncState = syncState,
        checksum = checksum
    )
}
