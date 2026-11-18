package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.NotificationChannelManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.SoundCategory
import com.example.domain.WidgetThemeMode
import com.example.widget.CompactPrayerWidgetProvider
import com.example.widget.DynamicDayPhase
import com.example.widget.PrayerScheduleWidgetProvider
import com.example.widget.WidgetThemeHelper
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Pengingat Shalat & Imsak`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pengingat Shalat & Imsak", appName)
  }

  @Test
  fun `channel rotation deletes old versioned channels and creates new ones`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val prefsV1 = PrayerPreferences(channelVersion = 1)
    NotificationChannelManager.ensureAllChannels(context, prefsV1)
    assertNotNull(
      nm.getNotificationChannel(NotificationChannelManager.channelIdFor(SoundCategory.IMSAK_ENTRY, 1))
    )

    NotificationChannelManager.rotateAndCreateChannels(
      context = context,
      preferences = prefsV1,
      oldVersion = 1,
      newVersion = 2,
    )

    assertNull(
      nm.getNotificationChannel(NotificationChannelManager.channelIdFor(SoundCategory.IMSAK_ENTRY, 1))
    )
    assertNotNull(
      nm.getNotificationChannel(NotificationChannelManager.channelIdFor(SoundCategory.IMSAK_ENTRY, 2))
    )
  }

  @Test
  fun `rescheduleAll and cancelRemindersForPrayer execute cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PrayerPreferences()
    PrayerAlarmScheduler.rescheduleAll(context, prefs)
    PrayerAlarmScheduler.cancelRemindersForPrayer(context, 2026, 11, 18, PrayerType.DZUHUR)
  }

  @Test
  fun `both 2x2 widgets build RemoteViews cleanly across default and mosque themes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    WidgetThemeMode.entries.forEach { mode ->
      val prefs = PrayerPreferences(widgetThemeMode = mode)
      val gridViews = PrayerScheduleWidgetProvider.buildRemoteViews(context, prefs)
      assertNotNull(gridViews)
      assertEquals(R.layout.widget_prayer_schedule, gridViews.layoutId)

      val compactViews = CompactPrayerWidgetProvider.buildRemoteViews(context, prefs)
      assertNotNull(compactViews)
      assertEquals(R.layout.widget_prayer_compact, compactViews.layoutId)
    }
  }

  @Test
  fun `dynamic mosque theme resolves dawn day sunset and night by local hour`() {
    val tzId = "Asia/Jakarta"
    fun millisAt(hour: Int, minute: Int): Long =
      Calendar.getInstance(TimeZone.getTimeZone(tzId))
        .apply {
          set(2026, Calendar.NOVEMBER, 18, hour, minute, 0)
        }
        .timeInMillis

    assertEquals(DynamicDayPhase.DAWN, WidgetThemeHelper.resolveDynamicPhase(millisAt(4, 30), tzId))
    assertEquals(DynamicDayPhase.DAY, WidgetThemeHelper.resolveDynamicPhase(millisAt(11, 0), tzId))
    assertEquals(DynamicDayPhase.SUNSET, WidgetThemeHelper.resolveDynamicPhase(millisAt(17, 30), tzId))
    assertEquals(DynamicDayPhase.NIGHT, WidgetThemeHelper.resolveDynamicPhase(millisAt(21, 0), tzId))
  }

  @Test
  fun `resolveColorScheme adapts app palette by time of day and preserves default theme`() {
    val defaultLight =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = false,
        themeMode = WidgetThemeMode.DEFAULT,
        dynamicPhase = DynamicDayPhase.NIGHT,
      )
    val defaultDark =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = true,
        themeMode = WidgetThemeMode.DEFAULT,
        dynamicPhase = DynamicDayPhase.DAY,
      )
    val dynamicDay =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = true,
        themeMode = WidgetThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.DAY,
      )
    val dynamicSunset =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = false,
        themeMode = WidgetThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.SUNSET,
      )
    val dynamicNight =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = false,
        themeMode = WidgetThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.NIGHT,
      )

    org.junit.Assert.assertNotEquals(defaultLight.background, defaultDark.background)
    org.junit.Assert.assertNotEquals(defaultLight.primary, dynamicDay.primary)
    org.junit.Assert.assertNotEquals(dynamicDay.background, dynamicNight.background)
    org.junit.Assert.assertNotEquals(dynamicSunset.primary, dynamicNight.primary)
  }
}
