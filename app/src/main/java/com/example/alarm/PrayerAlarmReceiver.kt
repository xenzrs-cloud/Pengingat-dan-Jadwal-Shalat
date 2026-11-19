package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PrayerPreferencesRepository
import com.example.domain.PrayerType
import com.example.domain.QuickSoundOverride
import com.example.domain.SoundCategory
import com.example.domain.SoundMode
import com.example.widget.PrayerScheduleWidgetProvider
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val appContext = context.applicationContext
    when (intent.action) {
      ACTION_STOP_ALARM,
      ACTION_DISMISS_PREVIEW -> {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        PrayerAlarmSoundService.stopAlarm(appContext, notificationId)
        return
      }

      ACTION_MARK_ALREADY_PRAYED -> {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        PrayerAlarmSoundService.stopAlarm(appContext, notificationId)

        val prayerId = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: return
        val prayerType = PrayerType.fromId(prayerId) ?: return
        val year = intent.getIntExtra(EXTRA_YEAR, 0)
        val month = intent.getIntExtra(EXTRA_MONTH, 0)
        val day = intent.getIntExtra(EXTRA_DAY, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
          try {
            if (year > 0 && month > 0 && day > 0) {
              val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
              val repo = PrayerPreferencesRepository(appContext)
              repo.setAlreadyPrayed(prayerType, true, dateKey)
              PrayerAlarmScheduler.cancelRemindersForPrayer(appContext, year, month, day, prayerType)
              val updatedPrefs = repo.getCurrent()
              runCatching {
                PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(appContext, updatedPrefs)
              }
            }
          } finally {
            pendingResult.finish()
          }
        }
        return
      }
    }

    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
    val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
    val title = intent.getStringExtra(EXTRA_TITLE) ?: "Pengingat Shalat"
    val body = intent.getStringExtra(EXTRA_BODY) ?: ""
    val isUrgent = intent.getBooleanExtra(EXTRA_IS_URGENT, false)
    val alarmKindCode =
      intent.getIntExtra(EXTRA_ALARM_KIND, PrayerAlarmScheduler.AlarmKind.ENTRY.code)
    val prayerType = intent.getStringExtra(EXTRA_PRAYER_TYPE)?.let { PrayerType.fromId(it) }
    val year = intent.getIntExtra(EXTRA_YEAR, 0)
    val month = intent.getIntExtra(EXTRA_MONTH, 0)
    val day = intent.getIntExtra(EXTRA_DAY, 0)

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val prefs = PrayerPreferencesRepository(appContext).getCurrent()
        runCatching { NotificationChannelManager.ensureAllChannels(appContext, prefs) }
        runCatching { PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(appContext, prefs) }

        val category =
          when {
            prayerType == PrayerType.IMSAK -> SoundCategory.IMSAK_ENTRY
            alarmKindCode == PrayerAlarmScheduler.AlarmKind.REMINDER_H30.code ||
              alarmKindCode == PrayerAlarmScheduler.AlarmKind.REMINDER_H15.code ->
              SoundCategory.EXPIRY_REMINDER
            else -> SoundCategory.PRAYER_ENTRY
          }

        val quickOverride =
          prayerType?.let { prefs.overrideFor(it) } ?: QuickSoundOverride.DEFAULT
        val setting = prefs.settingFor(category)
        val hasAudibleSound =
          quickOverride == QuickSoundOverride.DEFAULT &&
            (setting.mode == SoundMode.DEFAULT_ADZAN || setting.mode == SoundMode.CUSTOM_FILE)

        // Khusus masuk waktu shalat wajib (Adzan): putar 1x penuh sampai audio habis (tidak diulang terus).
        // Untuk H-30, H-15, dan Imsak: suara tetap diulang sampai notifikasi ditutup/di-swipe.
        val isObligatoryPrayerEntry =
          alarmKindCode == PrayerAlarmScheduler.AlarmKind.ENTRY.code &&
            prayerType != null &&
            prayerType.isObligatory
        val loopAudio = !isObligatoryPrayerEntry

        var startedService = false
        if (hasAudibleSound) {
          startedService =
            PrayerAlarmSoundService.startAlarm(
              context = appContext,
              notificationId = notificationId,
              title = title,
              body = body,
              isUrgent = isUrgent,
              appThemeMode = prefs.appThemeMode,
              timeZoneId = prefs.timeZoneId,
              prayerType = prayerType,
              year = year,
              month = month,
              day = day,
              isTestPreview = false,
              soundMode = setting.mode,
              customFileName = setting.customFileName,
              loopAudio = loopAudio,
            )
        }

        if (!startedService) {
          NotificationThemeHelper.buildAndShowNotification(
            context = appContext,
            notificationId = notificationId,
            channelId = channelId,
            title = title,
            body = body,
            isUrgent = isUrgent,
            appThemeMode = prefs.appThemeMode,
            timeZoneId = prefs.timeZoneId,
            prayerType = prayerType,
            year = year,
            month = month,
            day = day,
            isTestPreview = false,
            isInsistent = hasAudibleSound && loopAudio,
          )
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_MARK_ALREADY_PRAYED = "com.example.alarm.ACTION_MARK_ALREADY_PRAYED"
    const val ACTION_DISMISS_PREVIEW = "com.example.alarm.ACTION_DISMISS_PREVIEW"
    const val ACTION_STOP_ALARM = "com.example.alarm.ACTION_STOP_ALARM"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_CHANNEL_ID = "extra_channel_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_IS_URGENT = "extra_is_urgent"
    const val EXTRA_ALARM_KIND = "extra_alarm_kind"
    const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
    const val EXTRA_YEAR = "extra_year"
    const val EXTRA_MONTH = "extra_month"
    const val EXTRA_DAY = "extra_day"
  }
}
