package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.DailyPrayerSchedule
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.SoundCategory
import com.example.widget.PrayerScheduleWidgetProvider

object PrayerAlarmScheduler {

  enum class AlarmKind(val code: Int) {
    ENTRY(0),
    REMINDER_H30(1),
    REMINDER_H15(2),
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
  }

  fun rescheduleAll(
    context: Context,
    preferences: PrayerPreferences,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    NotificationChannelManager.ensureAllChannels(context, preferences)

    val (today, tomorrow) =
      KemenagPrayerCalculator.calculateTodayAndTomorrow(
        nowMillis = nowMillis,
        latitude = preferences.latitude,
        longitude = preferences.longitude,
        timeZoneId = preferences.timeZoneId,
        locationName = preferences.locationName,
      )

    scheduleForDay(context, today, preferences, nowMillis)
    scheduleForDay(context, tomorrow, preferences, nowMillis)
    runCatching { PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(context, preferences) }
  }

  fun scheduleForDay(
    context: Context,
    schedule: DailyPrayerSchedule,
    preferences: PrayerPreferences,
    nowMillis: Long,
  ) {
    val version = preferences.channelVersion

    // a) Alarm Waktu Imsak (10 menit sebelum Subuh)
    val imsakSlot = schedule.slotOf(PrayerType.IMSAK)
    val subuhSlot = schedule.slotOf(PrayerType.SUBUH)
    val subuhClock = KemenagPrayerCalculator.formatClock(subuhSlot.startEpochMillis, schedule.timeZoneId)
    val imsakOverride = preferences.overrideFor(PrayerType.IMSAK)
    val imsakChannel =
      NotificationChannelManager.resolveEffectiveChannelId(
        category = SoundCategory.IMSAK_ENTRY,
        override = imsakOverride,
        version = version,
      )

    scheduleOrCancel(
      context = context,
      triggerAtMillis = imsakSlot.startEpochMillis,
      nowMillis = nowMillis,
      schedule = schedule,
      prayerType = PrayerType.IMSAK,
      kind = AlarmKind.ENTRY,
      title = "Waktu Imsak Telah Tiba",
      body = "10 menit lagi menuju waktu Subuh (pukul $subuhClock)",
      channelId = imsakChannel,
    )

    // b, c, d) Alarm Waktu Masuk Shalat & Pengingat H-30 / H-15 untuk 5 shalat wajib
    PrayerType.obligatoryPrayers.forEach { prayerType ->
      val slot = schedule.slotOf(prayerType)
      val endMillis = slot.endEpochMillis ?: return@forEach
      val startClock = KemenagPrayerCalculator.formatClock(slot.startEpochMillis, schedule.timeZoneId)
      val endClock = KemenagPrayerCalculator.formatClock(endMillis, schedule.timeZoneId)
      val override = preferences.overrideFor(prayerType)

      val entryChannel =
        NotificationChannelManager.resolveEffectiveChannelId(
          category = SoundCategory.PRAYER_ENTRY,
          override = override,
          version = version,
        )
      val expiryChannel =
        NotificationChannelManager.resolveEffectiveChannelId(
          category = SoundCategory.EXPIRY_REMINDER,
          override = override,
          version = version,
        )

      // b) Waktu masuk shalat
      scheduleOrCancel(
        context = context,
        triggerAtMillis = slot.startEpochMillis,
        nowMillis = nowMillis,
        schedule = schedule,
        prayerType = prayerType,
        kind = AlarmKind.ENTRY,
        title = "Waktu Shalat ${prayerType.displayName} Telah Tiba",
        body = "Pukul $startClock • Batas waktu hingga $endClock (${schedule.locationName})",
        channelId = entryChannel,
      )

      val alreadyPrayed = preferences.isCheckedToday(prayerType, schedule.dateKey)
      val h30Millis = endMillis - 30 * 60_000L
      val h15Millis = endMillis - 15 * 60_000L

      if (alreadyPrayed) {
        cancelSingleAlarm(context, schedule.year, schedule.month, schedule.day, prayerType, AlarmKind.REMINDER_H30)
        cancelSingleAlarm(context, schedule.year, schedule.month, schedule.day, prayerType, AlarmKind.REMINDER_H15)
      } else {
        // c) Pengingat H-30 Menit Sebelum Habis
        if (h30Millis > slot.startEpochMillis) {
          scheduleOrCancel(
            context = context,
            triggerAtMillis = h30Millis,
            nowMillis = nowMillis,
            schedule = schedule,
            prayerType = prayerType,
            kind = AlarmKind.REMINDER_H30,
            title = "Pengingat Waktu Shalat ${prayerType.displayName}",
            body = "30 menit lagi waktu shalat ${prayerType.displayName} berakhir (pukul $endClock)",
            channelId = expiryChannel,
          )
        }
        // d) Pengingat H-15 Menit Sebelum Habis
        if (h15Millis > slot.startEpochMillis) {
          scheduleOrCancel(
            context = context,
            triggerAtMillis = h15Millis,
            nowMillis = nowMillis,
            schedule = schedule,
            prayerType = prayerType,
            kind = AlarmKind.REMINDER_H15,
            title = "Segera Shalat ${prayerType.displayName}!",
            body = "Segera shalat ${prayerType.displayName}! 15 menit lagi waktu shalat habis (pukul $endClock)",
            channelId = expiryChannel,
          )
        }
      }
    }
  }

  fun cancelRemindersForPrayer(
    context: Context,
    year: Int,
    month: Int,
    day: Int,
    prayerType: PrayerType,
  ) {
    cancelSingleAlarm(context, year, month, day, prayerType, AlarmKind.REMINDER_H30)
    cancelSingleAlarm(context, year, month, day, prayerType, AlarmKind.REMINDER_H15)

    // Also dismiss any active H-30 / H-15 notification from status bar immediately
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(requestCodeFor(year, month, day, prayerType, AlarmKind.REMINDER_H30))
    nm.cancel(requestCodeFor(year, month, day, prayerType, AlarmKind.REMINDER_H15))
  }

  private fun scheduleOrCancel(
    context: Context,
    triggerAtMillis: Long,
    nowMillis: Long,
    schedule: DailyPrayerSchedule,
    prayerType: PrayerType,
    kind: AlarmKind,
    title: String,
    body: String,
    channelId: String,
  ) {
    val reqCode = requestCodeFor(schedule.year, schedule.month, schedule.day, prayerType, kind)
    if (triggerAtMillis <= nowMillis + 1_000L) {
      cancelSingleAlarm(context, schedule.year, schedule.month, schedule.day, prayerType, kind)
      return
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent =
      Intent(context, PrayerAlarmReceiver::class.java).apply {
        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
        putExtra(PrayerAlarmReceiver.EXTRA_CHANNEL_ID, channelId)
        putExtra(PrayerAlarmReceiver.EXTRA_TITLE, title)
        putExtra(PrayerAlarmReceiver.EXTRA_BODY, body)
        putExtra(PrayerAlarmReceiver.EXTRA_IS_URGENT, kind == AlarmKind.REMINDER_H15)
      }

    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        reqCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    if (canScheduleExactAlarms(context)) {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }

  private fun cancelSingleAlarm(
    context: Context,
    year: Int,
    month: Int,
    day: Int,
    prayerType: PrayerType,
    kind: AlarmKind,
  ) {
    val reqCode = requestCodeFor(year, month, day, prayerType, kind)
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, PrayerAlarmReceiver::class.java)
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        reqCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (pendingIntent != null) {
      am.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun requestCodeFor(
    year: Int,
    month: Int,
    day: Int,
    prayerType: PrayerType,
    kind: AlarmKind,
  ): Int {
    val dayCompact = (year % 100) * 10000 + month * 100 + day // e.g. 261118
    return dayCompact * 100 + prayerType.ordinal * 10 + kind.code
  }
}
