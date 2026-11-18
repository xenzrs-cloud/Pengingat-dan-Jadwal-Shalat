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
 * Widget Opsi 1 (2x2): Jadwal Mini berisi 6 waktu harian (Imsak, Subuh, Dzuhur, Ashar, Maghrib, Isya)
 * dengan tema widget terpisah (tidak memengaruhi tema aplikasi).
 */
class PrayerScheduleWidgetProvider : AppWidgetProvider() {

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
    when (intent.action) {
      ACTION_REFRESH_WIDGET -> {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val prefs = PrayerPreferencesRepository(appContext).getCurrent()
            updateAllWidgetsWithPrefs(appContext, prefs)
          } finally {
            pendingResult.finish()
          }
        }
      }
      ACTION_CYCLE_THEME -> {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val repo = PrayerPreferencesRepository(appContext)
            val currentPrefs = repo.getCurrent()
            val nextTheme = WidgetThemeHelper.nextMode(currentPrefs.widgetThemeMode)
            repo.setWidgetThemeMode(nextTheme)
            updateAllWidgetsWithPrefs(appContext, currentPrefs.copy(widgetThemeMode = nextTheme))
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"
    const val ACTION_CYCLE_THEME = "com.example.widget.ACTION_CYCLE_THEME"

    fun updateAllWidgets(context: Context) {
      val appContext = context.applicationContext
      CoroutineScope(Dispatchers.IO).launch {
        val prefs = PrayerPreferencesRepository(appContext).getCurrent()
        updateAllWidgetsWithPrefs(appContext, prefs)
      }
    }

    fun updateAllWidgetsWithPrefs(context: Context, prefs: PrayerPreferences) {
      val manager = AppWidgetManager.getInstance(context)

      // Update Grid 2x2 Widgets
      val gridComponent = ComponentName(context, PrayerScheduleWidgetProvider::class.java)
      val gridIds = manager.getAppWidgetIds(gridComponent)
      if (gridIds.isNotEmpty()) {
        val views = buildRemoteViews(context, prefs)
        gridIds.forEach { widgetId -> manager.updateAppWidget(widgetId, views) }
      }

      // Update Medium 3x2 & Wide 4x2 Widgets
      MediumPrayerWidgetProvider.updateWithPrefs(context, manager, prefs)
      WidePrayerWidgetProvider.updateWithPrefs(context, manager, prefs)
    }

    fun buildRemoteViews(
      context: Context,
      prefs: PrayerPreferences,
      nowMillis: Long = System.currentTimeMillis(),
    ): RemoteViews {
      val (today, _) =
        KemenagPrayerCalculator.calculateTodayAndTomorrow(
          nowMillis = nowMillis,
          latitude = prefs.latitude,
          longitude = prefs.longitude,
          timeZoneId = prefs.timeZoneId,
          locationName = prefs.locationName,
        )

      val widgetPrayers =
        listOf(
          PrayerType.IMSAK,
          PrayerType.SUBUH,
          PrayerType.DZUHUR,
          PrayerType.ASHAR,
          PrayerType.MAGHRIB,
          PrayerType.ISYA,
        )

      val nextToday =
        widgetPrayers
          .map { today.slotOf(it) }
          .firstOrNull { it.startEpochMillis > nowMillis }

      val style = WidgetThemeHelper.resolveStyle(context, prefs.widgetThemeMode)

      val views = RemoteViews(context.packageName, R.layout.widget_prayer_schedule)

      if (style.backgroundImageRes != null) {
        views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE)
        val bitmap = WidgetThemeHelper.renderBackgroundBitmap(context, style.backgroundImageRes)
        if (bitmap != null) {
          views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
        } else {
          views.setImageViewResource(R.id.widget_bg_image, style.backgroundImageRes)
        }
      } else {
        views.setImageViewBitmap(R.id.widget_bg_image, null)
        views.setViewVisibility(R.id.widget_bg_image, View.GONE)
      }

      views.setTextViewText(R.id.widget_location, prefs.locationName)
      views.setTextColor(R.id.widget_location, style.primaryTextColor)
      views.setImageViewResource(R.id.widget_theme_btn, style.paletteIconRes)
      views.setImageViewResource(R.id.widget_refresh_btn, style.refreshIconRes)

      val timeViews =
        mapOf(
          PrayerType.IMSAK to R.id.widget_time_imsak,
          PrayerType.SUBUH to R.id.widget_time_subuh,
          PrayerType.DZUHUR to R.id.widget_time_dzuhur,
          PrayerType.ASHAR to R.id.widget_time_ashar,
          PrayerType.MAGHRIB to R.id.widget_time_maghrib,
          PrayerType.ISYA to R.id.widget_time_isya,
        )
      val labelViews =
        mapOf(
          PrayerType.IMSAK to R.id.widget_label_imsak,
          PrayerType.SUBUH to R.id.widget_label_subuh,
          PrayerType.DZUHUR to R.id.widget_label_dzuhur,
          PrayerType.ASHAR to R.id.widget_label_ashar,
          PrayerType.MAGHRIB to R.id.widget_label_maghrib,
          PrayerType.ISYA to R.id.widget_label_isya,
        )
      val slotContainers =
        mapOf(
          PrayerType.IMSAK to R.id.slot_imsak_container,
          PrayerType.SUBUH to R.id.slot_subuh_container,
          PrayerType.DZUHUR to R.id.slot_dzuhur_container,
          PrayerType.ASHAR to R.id.slot_ashar_container,
          PrayerType.MAGHRIB to R.id.slot_maghrib_container,
          PrayerType.ISYA to R.id.slot_isya_container,
        )

      widgetPrayers.forEach { type ->
        val clock =
          KemenagPrayerCalculator.formatClock(
            today.slotOf(type).startEpochMillis,
            prefs.timeZoneId,
          )
        val isNext = nextToday?.type == type
        val timeId = timeViews.getValue(type)
        val labelId = labelViews.getValue(type)
        val containerId = slotContainers.getValue(type)

        views.setTextViewText(timeId, clock)
        views.setTextColor(
          timeId,
          if (isNext) style.accentTextColor else style.primaryTextColor,
        )
        views.setTextColor(
          labelId,
          when {
            isNext -> style.accentTextColor
            type == PrayerType.IMSAK -> style.imsakLabelColor
            else -> style.secondaryTextColor
          },
        )
        views.setInt(
          containerId,
          "setBackgroundResource",
          if (isNext) style.slotActiveBgRes else style.slotDefaultBgRes,
        )
      }

      // Tap widget body -> open MainActivity
      val openIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val openPendingIntent =
        PendingIntent.getActivity(
          context,
          9001,
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

      // Tap palette icon -> cycle theme mode & sync both app and all widgets
      val themeIntent =
        Intent(context, PrayerScheduleWidgetProvider::class.java).apply {
          action = ACTION_CYCLE_THEME
        }
      val themePendingIntent =
        PendingIntent.getBroadcast(
          context,
          9005,
          themeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_theme_btn, themePendingIntent)

      // Tap refresh icon -> update all widgets immediately
      val refreshIntent =
        Intent(context, PrayerScheduleWidgetProvider::class.java).apply {
          action = ACTION_REFRESH_WIDGET
        }
      val refreshPendingIntent =
        PendingIntent.getBroadcast(
          context,
          9002,
          refreshIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

      return views
    }
  }
}
