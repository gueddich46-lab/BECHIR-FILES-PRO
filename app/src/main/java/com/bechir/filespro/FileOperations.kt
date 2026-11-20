package com.bechir.filespro

import androidx.documentfile.provider.DocumentFile

/** Operations on folders explicitly granted by the Android folder picker. */
object FileOperations {
    fun createFolder(parent: DocumentFile, name: String): Boolean {
        val safeName = name.trim()
        if (safeName.isEmpty() || safeName.contains('/')) return false
        return try { parent.createDirectory(safeName) != null } catch (_: Exception) { false }
    }

    fun rename(item: DocumentFile, newName: String): Boolean {
        val safeName = newName.trim()
        if (safeName.isEmpty() || safeName.contains('/')) return false
        return try { item.renameTo(safeName) } catch (_: Exception) { false }
    }

    /** Destructive: call only after explicit user confirmation. */
    fun deleteConfirmed(item: DocumentFile): Boolean {
        return try { item.delete() } catch (_: Exception) { false }
    }
}
