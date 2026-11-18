package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.NotificationChannelManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.PrayerPreferencesRepository
import com.example.domain.AppThemeMode
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.SoundCategory
import com.example.domain.WidgetThemeMode
import com.example.widget.DynamicDayPhase
import com.example.widget.MediumPrayerWidgetProvider
import com.example.widget.PrayerScheduleWidgetProvider
import com.example.widget.WidePrayerWidgetProvider
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
  fun `all 3 widget sizes 2x2 3x2 and 4x2 build RemoteViews cleanly across static widget themes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    WidgetThemeMode.entries.forEach { mode ->
      val prefs = PrayerPreferences(widgetThemeMode = mode)
      val views2x2 = PrayerScheduleWidgetProvider.buildRemoteViews(context, prefs)
      assertNotNull(views2x2)
      assertEquals(R.layout.widget_prayer_schedule, views2x2.layoutId)

      val views3x2 = MediumPrayerWidgetProvider.buildRemoteViews(context, prefs)
      assertNotNull(views3x2)
      assertEquals(R.layout.widget_prayer_3x2, views3x2.layoutId)

      val views4x2 = WidePrayerWidgetProvider.buildRemoteViews(context, prefs)
      assertNotNull(views4x2)
      assertEquals(R.layout.widget_prayer_4x2, views4x2.layoutId)
    }
  }

  @Test
  fun `dynamic app theme resolves dawn day sunset and night by local hour`() {
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
        themeMode = AppThemeMode.DEFAULT,
        dynamicPhase = DynamicDayPhase.NIGHT,
      )
    val defaultDark =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = true,
        themeMode = AppThemeMode.DEFAULT,
        dynamicPhase = DynamicDayPhase.DAY,
      )
    val dynamicDay =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = true,
        themeMode = AppThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.DAY,
      )
    val dynamicSunset =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = false,
        themeMode = AppThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.SUNSET,
      )
    val dynamicNight =
      com.example.ui.theme.resolveColorScheme(
        darkTheme = false,
        themeMode = AppThemeMode.DYNAMIC_MOSQUE,
        dynamicPhase = DynamicDayPhase.NIGHT,
      )

    org.junit.Assert.assertNotEquals(defaultLight.background, defaultDark.background)
    org.junit.Assert.assertNotEquals(defaultLight.primary, dynamicDay.primary)
    org.junit.Assert.assertNotEquals(dynamicDay.background, dynamicNight.background)
    org.junit.Assert.assertNotEquals(dynamicSunset.primary, dynamicNight.primary)
    org.junit.Assert.assertNotEquals(androidx.compose.ui.graphics.Color.White, defaultLight.surface)
    org.junit.Assert.assertNotEquals(androidx.compose.ui.graphics.Color.White, dynamicDay.surface)
    org.junit.Assert.assertNotEquals(androidx.compose.ui.graphics.Color.White, dynamicSunset.surface)
  }

  @Test
  fun `nextMode cycles all static widget theme modes and wraps cleanly`() {
    var mode = WidgetThemeMode.DEFAULT
    val visited = mutableListOf<WidgetThemeMode>()
    repeat(WidgetThemeMode.entries.size) {
      visited.add(mode)
      mode = WidgetThemeHelper.nextMode(mode)
    }
    assertEquals(WidgetThemeMode.entries, visited)
    assertEquals(WidgetThemeMode.DEFAULT, mode)
  }

  @Test
  fun `renderBackgroundBitmap produces non-null 256px bitmap for mosque drawables`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nightBitmap =
      WidgetThemeHelper.renderBackgroundBitmap(context, R.drawable.bg_widget_mosque_night)
    assertNotNull(nightBitmap)
    assertEquals(256, nightBitmap!!.width)
    assertEquals(256, nightBitmap.height)
  }

  @Test
  fun `cycling widget theme updates only widgetThemeMode without changing appThemeMode`() {
    kotlinx.coroutines.runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val repo = PrayerPreferencesRepository(context)
      repo.setAppThemeMode(AppThemeMode.DYNAMIC_MOSQUE)
      repo.setWidgetThemeMode(WidgetThemeMode.DEFAULT)

      PrayerScheduleWidgetProvider()
        .onReceive(context, Intent(PrayerScheduleWidgetProvider.ACTION_CYCLE_THEME))

      var current = repo.getCurrent()
      var attempts = 0
      while (current.widgetThemeMode == WidgetThemeMode.DEFAULT && attempts < 30) {
        kotlinx.coroutines.delay(50)
        current = repo.getCurrent()
        attempts++
      }
      assertEquals(WidgetThemeMode.MOSQUE_DAY, current.widgetThemeMode)
      assertEquals(AppThemeMode.DYNAMIC_MOSQUE, current.appThemeMode)

      MediumPrayerWidgetProvider()
        .onReceive(context, Intent(MediumPrayerWidgetProvider.ACTION_CYCLE_3X2_THEME))

      attempts = 0
      while (current.widgetThemeMode == WidgetThemeMode.MOSQUE_DAY && attempts < 30) {
        kotlinx.coroutines.delay(50)
        current = repo.getCurrent()
        attempts++
      }
      assertEquals(WidgetThemeMode.MOSQUE_SUNSET, current.widgetThemeMode)
      assertEquals(AppThemeMode.DYNAMIC_MOSQUE, current.appThemeMode)

      WidePrayerWidgetProvider()
        .onReceive(context, Intent(WidePrayerWidgetProvider.ACTION_CYCLE_4X2_THEME))

      attempts = 0
      while (current.widgetThemeMode == WidgetThemeMode.MOSQUE_SUNSET && attempts < 30) {
        kotlinx.coroutines.delay(50)
        current = repo.getCurrent()
        attempts++
      }
      assertEquals(WidgetThemeMode.MOSQUE_NIGHT, current.widgetThemeMode)
      assertEquals(AppThemeMode.DYNAMIC_MOSQUE, current.appThemeMode)
    }
  }
}
