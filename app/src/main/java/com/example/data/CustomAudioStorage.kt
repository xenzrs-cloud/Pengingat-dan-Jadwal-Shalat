package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.domain.SoundCategory
import java.io.File
import java.io.FileOutputStream

data class CopiedAudioResult(
  val savedFileName: String,
  val displayName: String,
)

object CustomAudioStorage {

  private const val CUSTOM_AUDIO_DIR = "custom_audio"

  fun getAudioDirectory(context: Context): File {
    val dir = File(context.filesDir, CUSTOM_AUDIO_DIR)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun copyPickedAudioToInternal(
    context: Context,
    category: SoundCategory,
    sourceUri: Uri,
  ): CopiedAudioResult? {
    val displayName = resolveDisplayName(context, sourceUri) ?: "audio_pilihan.mp3"
    val ext = resolveExtension(context, sourceUri, displayName)
    val dir = getAudioDirectory(context)

    // Remove previous audio file for this category so internal storage stays clean
    dir.listFiles()?.forEach { file ->
      if (file.name.startsWith("cat_${category.id}_")) {
        file.delete()
      }
    }

    val safeFileName = "cat_${category.id}_${System.currentTimeMillis()}.$ext"
    val destFile = File(dir, safeFileName)

    return runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: return null
        if (!destFile.exists() || destFile.length() == 0L) {
          destFile.delete()
          return null
        }
        CopiedAudioResult(savedFileName = safeFileName, displayName = displayName)
      }
      .getOrNull()
  }

  fun getSavedAudioFile(context: Context, fileName: String?): File? {
    if (fileName.isNullOrBlank()) return null
    val file = File(getAudioDirectory(context), fileName)
    return if (file.exists() && file.length() > 0L) file else null
  }

  fun getFileProviderUri(context: Context, fileName: String?): Uri? {
    val file = getSavedAudioFile(context, fileName) ?: return null
    return runCatching {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        // Grant read permission to SystemUI so NotificationManager can read the FileProvider URI safely
        runCatching {
          context.grantUriPermission(
            "com.android.systemui",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        uri
      }
      .getOrNull()
  }

  private fun resolveDisplayName(context: Context, uri: Uri): String? {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) {
              val name = cursor.getString(idx)
              if (!name.isNullOrBlank()) return name
            }
          }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
  }

  private fun resolveExtension(context: Context, uri: Uri, displayName: String): String {
    val fromName = displayName.substringAfterLast('.', "").lowercase().filter { it.isLetterOrDigit() }
    if (fromName.isNotBlank() && fromName.length <= 5) return fromName
    val mimeType = context.contentResolver.getType(uri)
    val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    return fromMime?.takeIf { it.isNotBlank() } ?: "mp3"
  }
}
