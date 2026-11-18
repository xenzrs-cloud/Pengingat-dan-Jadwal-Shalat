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
 * Widget Opsi 3 (4x2): Banner lebar dengan ringkasan shalat berikutnya serta 6 jadwal waktu lega.
 * Menggunakan tema widget terpisah (tidak memengaruhi tema aplikasi).
 */
class WidePrayerWidgetProvider : AppWidgetProvider() {

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
        val views = buildRemoteViews(appContext, prefs)
        appWidgetIds.forEach { widgetId -> appWidgetManager.updateAppWidget(widgetId, views) }
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    when (intent.action) {
      ACTION_REFRESH_4X2_WIDGET -> {
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
      ACTION_CYCLE_4X2_THEME -> {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val repo = PrayerPreferencesRepository(appContext)
            val currentPrefs = repo.getCurrent()
            val nextTheme = WidgetThemeHelper.nextMode(currentPrefs.widgetThemeMode)
            repo.setWidgetThemeMode(nextTheme)
            PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(
              appContext,
              currentPrefs.copy(widgetThemeMode = nextTheme),
            )
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_4X2_WIDGET = "com.example.widget.ACTION_REFRESH_4X2_WIDGET"
    const val ACTION_CYCLE_4X2_THEME = "com.example.widget.ACTION_CYCLE_4X2_THEME"

    fun updateWithPrefs(
      context: Context,
      manager: AppWidgetManager,
      prefs: PrayerPreferences,
    ) {
      val componentName = ComponentName(context, WidePrayerWidgetProvider::class.java)
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

      val subtitleText =
        buildString {
          append(if (nextToday != null) "Berikutnya: " else "Besok: ")
          append("${nextSlot.type.displayName} $nextClock")
          if (activeSlot != null && activeSlot.endEpochMillis != null) {
            val endClock =
              KemenagPrayerCalculator.formatClock(activeSlot.endEpochMillis, prefs.timeZoneId)
            append(" • ${activeSlot.type.displayName} habis $endClock")
          }
        }

      val style = WidgetThemeHelper.resolveStyle(context, prefs.widgetThemeMode)
      val views = RemoteViews(context.packageName, R.layout.widget_prayer_4x2)

      if (style.backgroundImageRes != null) {
        views.setViewVisibility(R.id.widget4x2_bg_image, View.VISIBLE)
        val bitmap = WidgetThemeHelper.renderBackgroundBitmap(context, style.backgroundImageRes)
        if (bitmap != null) {
          views.setImageViewBitmap(R.id.widget4x2_bg_image, bitmap)
        } else {
          views.setImageViewResource(R.id.widget4x2_bg_image, style.backgroundImageRes)
        }
      } else {
        views.setImageViewBitmap(R.id.widget4x2_bg_image, null)
        views.setViewVisibility(R.id.widget4x2_bg_image, View.GONE)
      }

      views.setTextViewText(R.id.widget4x2_location, prefs.locationName)
      views.setTextColor(R.id.widget4x2_location, style.primaryTextColor)
      views.setTextViewText(R.id.widget4x2_subtitle, subtitleText)
      views.setTextColor(R.id.widget4x2_subtitle, style.accentTextColor)
      views.setImageViewResource(R.id.widget4x2_theme_btn, style.paletteIconRes)
      views.setImageViewResource(R.id.widget4x2_refresh_btn, style.refreshIconRes)

      val timeViews =
        mapOf(
          PrayerType.IMSAK to R.id.widget4x2_time_imsak,
          PrayerType.SUBUH to R.id.widget4x2_time_subuh,
          PrayerType.DZUHUR to R.id.widget4x2_time_dzuhur,
          PrayerType.ASHAR to R.id.widget4x2_time_ashar,
          PrayerType.MAGHRIB to R.id.widget4x2_time_maghrib,
          PrayerType.ISYA to R.id.widget4x2_time_isya,
        )
      val labelViews =
        mapOf(
          PrayerType.IMSAK to R.id.widget4x2_label_imsak,
          PrayerType.SUBUH to R.id.widget4x2_label_subuh,
          PrayerType.DZUHUR to R.id.widget4x2_label_dzuhur,
          PrayerType.ASHAR to R.id.widget4x2_label_ashar,
          PrayerType.MAGHRIB to R.id.widget4x2_label_maghrib,
          PrayerType.ISYA to R.id.widget4x2_label_isya,
        )
      val slotContainers =
        mapOf(
          PrayerType.IMSAK to R.id.widget4x2_slot_imsak,
          PrayerType.SUBUH to R.id.widget4x2_slot_subuh,
          PrayerType.DZUHUR to R.id.widget4x2_slot_dzuhur,
          PrayerType.ASHAR to R.id.widget4x2_slot_ashar,
          PrayerType.MAGHRIB to R.id.widget4x2_slot_maghrib,
          PrayerType.ISYA to R.id.widget4x2_slot_isya,
        )

      ordered.forEach { type ->
        val clock = KemenagPrayerCalculator.formatClock(today.slotOf(type).startEpochMillis, prefs.timeZoneId)
        val isNext = nextToday?.type == type
        views.setTextViewText(timeViews.getValue(type), clock)
        views.setTextColor(
          timeViews.getValue(type),
          if (isNext) style.accentTextColor else style.primaryTextColor,
        )
        views.setTextColor(
          labelViews.getValue(type),
          when {
            isNext -> style.accentTextColor
            type == PrayerType.IMSAK -> style.imsakLabelColor
            else -> style.secondaryTextColor
          },
        )
        views.setInt(
          slotContainers.getValue(type),
          "setBackgroundResource",
          if (isNext) style.slotActiveBgRes else style.slotDefaultBgRes,
        )
      }

      val openIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      views.setOnClickPendingIntent(
        R.id.widget4x2_root,
        PendingIntent.getActivity(
          context,
          9201,
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      val themeIntent =
        Intent(context, WidePrayerWidgetProvider::class.java).apply {
          action = ACTION_CYCLE_4X2_THEME
        }
      views.setOnClickPendingIntent(
        R.id.widget4x2_theme_btn,
        PendingIntent.getBroadcast(
          context,
          9202,
          themeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      val refreshIntent =
        Intent(context, WidePrayerWidgetProvider::class.java).apply {
          action = ACTION_REFRESH_4X2_WIDGET
        }
      views.setOnClickPendingIntent(
        R.id.widget4x2_refresh_btn,
        PendingIntent.getBroadcast(
          context,
          9203,
          refreshIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )

      return views
    }
  }
}
