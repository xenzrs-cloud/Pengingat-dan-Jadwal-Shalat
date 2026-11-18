package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.R
import com.example.data.CustomAudioStorage
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPreviewPlayer(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null

  private val _playingCategory = MutableStateFlow<SoundCategory?>(null)
  val playingCategory: StateFlow<SoundCategory?> = _playingCategory.asStateFlow()

  fun playPreview(
    category: SoundCategory,
    setting: SoundCategorySetting,
    onError: (String) -> Unit = {},
  ) {
    stop()
    when (setting.mode) {
      SoundMode.VIBRATE_ONLY,
      SoundMode.SILENT -> {
        onError("Mode saat ini tidak memakai suara.")
        return
      }
      SoundMode.DEFAULT_ADZAN -> playBuiltIn(category, onError)
      SoundMode.CUSTOM_FILE -> {
        val file = CustomAudioStorage.getSavedAudioFile(context, setting.customFileName)
        if (file == null) {
          onError("File audio belum dipilih. Memutar suara bawaan sebagai cadangan.")
          playBuiltIn(category, onError)
          return
        }
        runCatching {
            val player =
              MediaPlayer().apply {
                setAudioAttributes(
                  AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                  stop()
                  true
                }
                prepare()
                start()
              }
            mediaPlayer = player
            _playingCategory.value = category
          }
          .onFailure {
            stop()
            onError("Gagal memutar file lokal. Memutar suara bawaan.")
            playBuiltIn(category, onError)
          }
      }
    }
  }

  private fun playBuiltIn(category: SoundCategory, onError: (String) -> Unit) {
    runCatching {
        val player =
          MediaPlayer.create(context, R.raw.adzan)?.apply {
            setOnCompletionListener { stop() }
            setOnErrorListener { _, _, _ ->
              stop()
              true
            }
            start()
          }
        if (player == null) {
          onError("Aset suara bawaan tidak ditemukan.")
          return
        }
        mediaPlayer = player
        _playingCategory.value = category
      }
      .onFailure {
        stop()
        onError("Gagal memutar pratinjau suara.")
      }
  }

  fun stop() {
    runCatching {
      mediaPlayer?.let { player ->
        if (player.isPlaying) {
          player.stop()
        }
        player.release()
      }
    }
    mediaPlayer = null
    _playingCategory.value = null
  }
}
