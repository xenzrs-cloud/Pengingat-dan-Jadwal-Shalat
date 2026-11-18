package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.example.R
import com.example.data.CustomAudioStorage
import com.example.domain.PrayerPreferences
import com.example.domain.QuickSoundOverride
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode

/**
 * Manages versioned Android 8+ Notification Channels.
 * Because Android locks a NotificationChannel's sound & vibration once created,
 * saving sound settings increments the version suffix (_v1 -> _v2), deletes old channels,
 * and creates fresh channels with updated AudioAttributes.
 */
object NotificationChannelManager {

  fun channelIdFor(category: SoundCategory, version: Int): String =
    "channel_${category.id}_v$version"

  fun vibrateOverrideChannelId(version: Int): String = "channel_quick_vibrate_v$version"

  fun silentOverrideChannelId(version: Int): String = "channel_quick_silent_v$version"

  fun resolveEffectiveChannelId(
    category: SoundCategory,
    override: QuickSoundOverride,
    version: Int,
  ): String =
    when (override) {
      QuickSoundOverride.DEFAULT -> channelIdFor(category, version)
      QuickSoundOverride.VIBRATE -> vibrateOverrideChannelId(version)
      QuickSoundOverride.SILENT -> silentOverrideChannelId(version)
    }

  fun ensureAllChannels(context: Context, preferences: PrayerPreferences) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val v = preferences.channelVersion

    createCategoryChannel(
      context = context,
      nm = nm,
      channelId = channelIdFor(SoundCategory.PRAYER_ENTRY, v),
      name = "Alarm Masuk Shalat",
      description = "Notifikasi dan suara saat waktu shalat wajib tiba",
      setting = preferences.prayerEntrySound,
    )

    createCategoryChannel(
      context = context,
      nm = nm,
      channelId = channelIdFor(SoundCategory.IMSAK_ENTRY, v),
      name = "Alarm Waktu Imsak",
      description = "Pengingat waktu Imsak 10 menit sebelum Subuh",
      setting = preferences.imsakEntrySound,
    )

    createCategoryChannel(
      context = context,
      nm = nm,
      channelId = channelIdFor(SoundCategory.EXPIRY_REMINDER, v),
      name = "Pengingat Sebelum Waktu Habis",
      description = "Pengingat H-30 dan H-15 sebelum waktu shalat berakhir",
      setting = preferences.expiryReminderSound,
    )

    createOverrideChannels(nm, v)
  }

  fun rotateAndCreateChannels(
    context: Context,
    preferences: PrayerPreferences,
    oldVersion: Int,
    newVersion: Int,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Delete previous versioned channels so stale channel audio configs are cleaned up
    SoundCategory.entries.forEach { category ->
      runCatching { nm.deleteNotificationChannel(channelIdFor(category, oldVersion)) }
    }
    runCatching { nm.deleteNotificationChannel(vibrateOverrideChannelId(oldVersion)) }
    runCatching { nm.deleteNotificationChannel(silentOverrideChannelId(oldVersion)) }

    ensureAllChannels(context, preferences.copy(channelVersion = newVersion))
  }

  private fun createCategoryChannel(
    context: Context,
    nm: NotificationManager,
    channelId: String,
    name: String,
    description: String,
    setting: SoundCategorySetting,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val importance =
      when (setting.mode) {
        SoundMode.SILENT -> NotificationManager.IMPORTANCE_DEFAULT
        else -> NotificationManager.IMPORTANCE_HIGH
      }

    val channel =
      NotificationChannel(channelId, name, importance).apply {
        this.description = description
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }

    when (setting.mode) {
      SoundMode.DEFAULT_ADZAN -> {
        val soundUri = builtInSoundUri(context)
        channel.setSound(soundUri, alarmAudioAttributes())
        channel.enableVibration(true)
      }
      SoundMode.CUSTOM_FILE -> {
        val customUri = CustomAudioStorage.getFileProviderUri(context, setting.customFileName)
        val safeUri = customUri ?: builtInSoundUri(context)
        runCatching { channel.setSound(safeUri, alarmAudioAttributes()) }
          .onFailure {
            // Safe fallback to built-in audio if system rejects custom URI
            channel.setSound(builtInSoundUri(context), alarmAudioAttributes())
          }
        channel.enableVibration(true)
      }
      SoundMode.VIBRATE_ONLY -> {
        channel.setSound(null, null)
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0L, 450L, 250L, 450L)
      }
      SoundMode.SILENT -> {
        channel.setSound(null, null)
        channel.enableVibration(false)
      }
    }

    nm.createNotificationChannel(channel)
  }

  private fun createOverrideChannels(nm: NotificationManager, version: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val vibrateChannel =
      NotificationChannel(
          vibrateOverrideChannelId(version),
          "Pengingat Getar (Override Cepat)",
          NotificationManager.IMPORTANCE_HIGH,
        )
        .apply {
          description = "Saluran khusus untuk jadwal dengan mode cepat Getar"
          setSound(null, null)
          enableVibration(true)
          vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)
        }

    val silentChannel =
      NotificationChannel(
          silentOverrideChannelId(version),
          "Pengingat Hening (Override Cepat)",
          NotificationManager.IMPORTANCE_DEFAULT,
        )
        .apply {
          description = "Saluran khusus untuk jadwal dengan mode cepat Hening"
          setSound(null, null)
          enableVibration(false)
        }

    nm.createNotificationChannel(vibrateChannel)
    nm.createNotificationChannel(silentChannel)
  }

  private fun builtInSoundUri(context: Context): Uri =
    Uri.parse("android.resource://${context.packageName}/${R.raw.adzan}")

  private fun alarmAudioAttributes(): AudioAttributes =
    AudioAttributes.Builder()
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .setUsage(AudioAttributes.USAGE_ALARM)
      .build()
}
