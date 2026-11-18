package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PrayerPreferencesRepository
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget Opsi 2 (2x2): Shalat Berikutnya Hero — tampilan ringkas fokus pada
 * waktu shalat yang akan tiba berikutnya dan jadwal Imsak/aktif.
 */
class CompactPrayerWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val prefs = PrayerPreferencesRepository(appContext).getCurrent()
        appWidgetIds.forEach { widgetId ->
          val views = buildRemoteViews(appContext, prefs)
          appWidgetManager.updateAppWidget(widgetId, views)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH_COMPACT_WIDGET) {
      val pendingResult = goAsync()
      val appContext = context.applicationContext
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val prefs = PrayerPreferencesRepository(appContext).getCurrent()
          PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(appContext, prefs)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_COMPACT_WIDGET = "com.example.widget.ACTION_REFRESH_COMPACT_WIDGET"

    fun updateWithPrefs(
      context: Context,
      manager: AppWidgetManager,
      prefs: PrayerPreferences,
    ) {
      val componentName = ComponentName(context, CompactPrayerWidgetProvider::class.java)
      val widgetIds = manager.getAppWidgetIds(componentName)
      if (widgetIds.isEmpty()) return

      val views = buildRemoteViews(context, prefs)
      widgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, views) }
    }

    fun buildRemoteViews(
      context: Context,
      prefs: PrayerPreferences,
      nowMillis: Long = System.currentTimeMillis(),
    ): RemoteViews {
      val (today, tomorrow) =
        KemenagPrayerCalculator.calculateTodayAndTomorrow(
          nowMillis = nowMillis,
          latitude = prefs.latitude,
          longitude = prefs.longitude,
          timeZoneId = prefs.timeZoneId,
          locationName = prefs.locationName,
        )

      val ordered =
        listOf(
          PrayerType.IMSAK,
          PrayerType.SUBUH,
          PrayerType.DZUHUR,
          PrayerType.ASHAR,
          PrayerType.MAGHRIB,
          PrayerType.ISYA,
        )

      val nextToday = ordered.map { today.slotOf(it) }.firstOrNull { it.startEpochMillis > nowMillis }
      val nextSlot = nextToday ?: tomorrow.slotOf(PrayerType.IMSAK)
      val nextClock = KemenagPrayerCalculator.formatClock(nextSlot.startEpochMillis, prefs.timeZoneId)

      val activeSlot =
        PrayerType.obligatoryPrayers
          .map { today.slotOf(it) }
          .firstOrNull { slot ->
            val end = slot.endEpochMillis ?: return@firstOrNull false
            nowMillis in slot.startEpochMillis until end
          }

      val footerText =
        if (activeSlot != null && activeSlot.endEpochMillis != null) {
          val endClock = KemenagPrayerCalculator.formatClock(activeSlot.endEpochMillis, prefs.timeZoneId)
          "${activeSlot.type.displayName} aktif • Habis $endClock"
        } else {
          val imsakClock =
            KemenagPrayerCalculator.formatClock(
              today.slotOf(PrayerType.IMSAK).startEpochMillis,
              prefs.timeZoneId,
            )
          val subuhClock =
            KemenagPrayerCalculator.formatClock(
              today.slotOf(PrayerType.SUBUH).startEpochMillis,
              prefs.timeZoneId,
            )
          "Imsak $imsakClock • Subuh $subuhClock"
        }

      val style =
        WidgetThemeHelper.resolveStyle(
          context = context,
          mode = prefs.widgetThemeMode,
          nowMillis = nowMillis,
          timeZoneId = prefs.timeZoneId,
        )

      val views = RemoteViews(context.packageName, R.layout.widget_prayer_compact)

      if (style.backgroundImageRes != null) {
        views.setViewVisibility(R.id.compact_bg_image, View.VISIBLE)
        views.setImageViewResource(R.id.compact_bg_image, style.backgroundImageRes)
      } else {
        views.setViewVisibility(R.id.compact_bg_image, View.GONE)
      }

      views.setTextViewText(R.id.compact_location, prefs.locationName)
      views.setTextColor(R.id.compact_location, style.primaryTextColor)
      views.setImageViewResource(R.id.compact_refresh_btn, style.refreshIconRes)

      views.setTextViewText(
        R.id.compact_next_caption,
        if (nextToday != null) "Waktu Berikutnya" else "Besok • Berikutnya",
      )
      views.setTextColor(R.id.compact_next_caption, style.secondaryTextColor)

      views.setTextViewText(R.id.compact_next_name, nextSlot.type.displayName)
      views.setTextColor(R.id.compact_next_name, style.primaryTextColor)

      views.setTextViewText(R.id.compact_next_time, nextClock)
      views.setTextColor(R.id.compact_next_time, style.accentTextColor)

      views.setInt(R.id.compact_footer_container, "setBackgroundResource", style.slotDefaultBgRes)
      views.setTextViewText(R.id.compact_footer_text, footerText)
      views.setTextColor(R.id.compact_footer_text, style.primaryTextColor)

      val openIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val openPendingIntent =
        PendingIntent.getActivity(
          context,
          9003,
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.compact_widget_root, openPendingIntent)

      val refreshIntent =
        Intent(context, CompactPrayerWidgetProvider::class.java).apply {
          action = ACTION_REFRESH_COMPACT_WIDGET
        }
      val refreshPendingIntent =
        PendingIntent.getBroadcast(
          context,
          9004,
          refreshIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.compact_refresh_btn, refreshPendingIntent)

      return views
    }
  }
}
