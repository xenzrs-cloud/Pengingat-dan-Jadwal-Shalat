package com.example.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.CustomAudioStorage
import com.example.domain.AppThemeMode
import com.example.domain.PrayerType
import com.example.domain.SoundMode

/**
 * Plays the configured prayer alarm sound on a loop using `AudioAttributes.USAGE_ALARM`
 * so even short custom audio clips repeat continuously and remain audible when the phone
 * is in Silent/Vibrate mode, until the user swipes/closes the notification or marks Sudah Shalat.
 */
class PrayerAlarmSoundService : Service() {

  private var mediaPlayer: MediaPlayer? = null
  private var currentNotificationId: Int = 0

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_ALARM_SOUND) {
      val targetId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, currentNotificationId)
      stopPlayback()
      if (targetId != 0) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(targetId)
      }
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, 1001) ?: 1001
    val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Pengingat Shalat"
    val body = intent?.getStringExtra(EXTRA_BODY) ?: "Waktunya menunaikan shalat"
    val isUrgent = intent?.getBooleanExtra(EXTRA_IS_URGENT, false) ?: false
    val themeMode = AppThemeMode.fromId(intent?.getStringExtra(EXTRA_APP_THEME_MODE))
    val timeZoneId = intent?.getStringExtra(EXTRA_TIMEZONE_ID) ?: "Asia/Jakarta"
    val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE)?.let { PrayerType.fromId(it) }
    val year = intent?.getIntExtra(EXTRA_YEAR, 0) ?: 0
    val month = intent?.getIntExtra(EXTRA_MONTH, 0) ?: 0
    val day = intent?.getIntExtra(EXTRA_DAY, 0) ?: 0
    val isTestPreview = intent?.getBooleanExtra(EXTRA_IS_TEST_PREVIEW, false) ?: false
    val soundMode = SoundMode.fromId(intent?.getStringExtra(EXTRA_SOUND_MODE))
    val customFileName = intent?.getStringExtra(EXTRA_CUSTOM_FILE_NAME)
    val loopAudio = intent?.getBooleanExtra(EXTRA_LOOP_AUDIO, true) ?: true
    val hasAudibleSound = soundMode == SoundMode.DEFAULT_ADZAN || soundMode == SoundMode.CUSTOM_FILE

    val bannerChannelId = NotificationChannelManager.ensureAlarmBannerChannel(this)
    val notification =
      NotificationThemeHelper.buildAlarmNotification(
        context = this,
        notificationId = notificationId,
        channelId = bannerChannelId,
        title = title,
        body = body,
        isUrgent = isUrgent,
        appThemeMode = themeMode,
        timeZoneId = timeZoneId,
        prayerType = prayerType,
        year = year,
        month = month,
        day = day,
        isTestPreview = isTestPreview,
        isInsistent = loopAudio && hasAudibleSound,
      )

    if (currentNotificationId != 0 && currentNotificationId != notificationId) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.cancel(currentNotificationId)
    }
    currentNotificationId = notificationId

    val fgType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
      } else {
        0
      }
    ServiceCompat.startForeground(this, notificationId, notification, fgType)

    startAlarmAudio(soundMode, customFileName, loopAudio)
    return if (loopAudio) START_STICKY else START_NOT_STICKY
  }

  private fun startAlarmAudio(soundMode: SoundMode, customFileName: String?, loopAudio: Boolean) {
    stopPlayback()
    if (soundMode == SoundMode.VIBRATE_ONLY || soundMode == SoundMode.SILENT) {
      return
    }

    val alarmAttributes =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    val customFile =
      if (soundMode == SoundMode.CUSTOM_FILE) {
        CustomAudioStorage.getSavedAudioFile(this, customFileName)
      } else {
        null
      }

    if (customFile != null) {
      val startedCustom =
        runCatching {
            val player =
              MediaPlayer().apply {
                setAudioAttributes(alarmAttributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(customFile.absolutePath)
                isLooping = loopAudio
                if (!loopAudio) {
                  setOnCompletionListener { finishAudioPlaybackKeepingNotification() }
                }
                setOnErrorListener { _, _, _ ->
                  finishAudioPlaybackKeepingNotification()
                  true
                }
                prepare()
                start()
              }
            mediaPlayer = player
            true
          }
          .getOrDefault(false)
      if (startedCustom) return
    }

    // Fallback or DEFAULT_ADZAN: play built-in adzan on STREAM_ALARM
    runCatching {
      val afd = resources.openRawResourceFd(R.raw.adzan) ?: return
      afd.use { descriptor ->
        val player =
          MediaPlayer().apply {
            setAudioAttributes(alarmAttributes)
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            isLooping = loopAudio
            if (!loopAudio) {
              setOnCompletionListener { finishAudioPlaybackKeepingNotification() }
            }
            setOnErrorListener { _, _, _ ->
              finishAudioPlaybackKeepingNotification()
              true
            }
            prepare()
            start()
          }
        mediaPlayer = player
      }
    }
  }

  private fun finishAudioPlaybackKeepingNotification() {
    stopPlayback()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    stopSelf()
  }

  private fun stopPlayback() {
    runCatching {
      mediaPlayer?.let { player ->
        if (player.isPlaying) {
          player.stop()
        }
        player.release()
      }
    }
    mediaPlayer = null
  }

  override fun onDestroy() {
    stopPlayback()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START_ALARM_SOUND = "com.example.alarm.ACTION_START_ALARM_SOUND"
    const val ACTION_STOP_ALARM_SOUND = "com.example.alarm.ACTION_STOP_ALARM_SOUND"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_IS_URGENT = "extra_is_urgent"
    const val EXTRA_APP_THEME_MODE = "extra_app_theme_mode"
    const val EXTRA_TIMEZONE_ID = "extra_timezone_id"
    const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
    const val EXTRA_YEAR = "extra_year"
    const val EXTRA_MONTH = "extra_month"
    const val EXTRA_DAY = "extra_day"
    const val EXTRA_IS_TEST_PREVIEW = "extra_is_test_preview"
    const val EXTRA_SOUND_MODE = "extra_sound_mode"
    const val EXTRA_CUSTOM_FILE_NAME = "extra_custom_file_name"
    const val EXTRA_LOOP_AUDIO = "extra_loop_audio"

    fun startAlarm(
      context: Context,
      notificationId: Int,
      title: String,
      body: String,
      isUrgent: Boolean,
      appThemeMode: AppThemeMode,
      timeZoneId: String,
      prayerType: PrayerType?,
      year: Int = 0,
      month: Int = 0,
      day: Int = 0,
      isTestPreview: Boolean = false,
      soundMode: SoundMode,
      customFileName: String?,
      loopAudio: Boolean = true,
    ): Boolean {
      val intent =
        Intent(context, PrayerAlarmSoundService::class.java).apply {
          action = ACTION_START_ALARM_SOUND
          putExtra(EXTRA_NOTIFICATION_ID, notificationId)
          putExtra(EXTRA_TITLE, title)
          putExtra(EXTRA_BODY, body)
          putExtra(EXTRA_IS_URGENT, isUrgent)
          putExtra(EXTRA_APP_THEME_MODE, appThemeMode.id)
          putExtra(EXTRA_TIMEZONE_ID, timeZoneId)
          putExtra(EXTRA_PRAYER_TYPE, prayerType?.id)
          putExtra(EXTRA_YEAR, year)
          putExtra(EXTRA_MONTH, month)
          putExtra(EXTRA_DAY, day)
          putExtra(EXTRA_IS_TEST_PREVIEW, isTestPreview)
          putExtra(EXTRA_SOUND_MODE, soundMode.id)
          putExtra(EXTRA_CUSTOM_FILE_NAME, customFileName)
          putExtra(EXTRA_LOOP_AUDIO, loopAudio)
        }
      return runCatching {
          ContextCompat.startForegroundService(context, intent)
          true
        }
        .getOrDefault(false)
    }

    fun stopAlarm(context: Context, notificationId: Int = 0) {
      val stopIntent =
        Intent(context, PrayerAlarmSoundService::class.java).apply {
          action = ACTION_STOP_ALARM_SOUND
          putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
      runCatching { context.startService(stopIntent) }
      if (notificationId != 0) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
      }
    }
  }
}
