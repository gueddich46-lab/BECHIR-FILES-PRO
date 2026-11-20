package com.bechir.filespro

import androidx.documentfile.provider.DocumentFile
import android.content.Context

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
    /** Copy a regular file; refuse collisions to avoid overwriting existing data. */
    fun copyFile(context: Context, source: DocumentFile, destination: DocumentFile): Boolean {
        if (!source.isFile || !destination.isDirectory) return false
        val name = source.name ?: return false
        if (destination.findFile(name) != null) return false
        val created = destination.createFile(source.type ?: "application/octet-stream", name) ?: return false
        return try {
            val input = context.contentResolver.openInputStream(source.uri) ?: error("Cannot read")
            input.use { stream ->
                val output = context.contentResolver.openOutputStream(created.uri, "w") ?: error("Cannot write")
                output.use { stream.copyTo(it) }
            }
            true
        } catch (_: Exception) {
            created.delete()
            false
        }
    }

    /** Move only after a successful copy; never remove the source on copy failure. */
    fun moveFile(context: Context, source: DocumentFile, destination: DocumentFile): Boolean {
        if (!copyFile(context, source, destination)) return false
        return source.delete()
    }

}
