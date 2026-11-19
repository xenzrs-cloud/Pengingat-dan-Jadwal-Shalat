package com.example

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.NotificationChannelManager
import com.example.alarm.NotificationThemeHelper
import com.example.alarm.PrayerAlarmReceiver
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmSoundService
import com.example.data.PrayerPreferencesRepository
import com.example.domain.AppThemeMode
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode
import com.example.domain.WidgetThemeMode
import com.example.widget.DynamicDayPhase
import com.example.widget.MediumPrayerWidgetProvider
import com.example.widget.PrayerScheduleWidgetProvider
import com.example.widget.WidePrayerWidgetProvider
import com.example.widget.WidgetThemeHelper
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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

    assertNotEquals(defaultLight.background, defaultDark.background)
    assertNotEquals(defaultLight.primary, dynamicDay.primary)
    assertNotEquals(dynamicDay.background, dynamicNight.background)
    assertNotEquals(dynamicSunset.primary, dynamicNight.primary)
    assertNotEquals(androidx.compose.ui.graphics.Color.White, defaultLight.surface)
    assertNotEquals(androidx.compose.ui.graphics.Color.White, dynamicDay.surface)
    assertNotEquals(androidx.compose.ui.graphics.Color.White, dynamicSunset.surface)
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

  @Test
  fun `notification theme helper resolves distinct visuals for app themes and dynamic phases`() {
    val tzId = "Asia/Jakarta"
    fun millisAt(hour: Int, minute: Int): Long =
      Calendar.getInstance(TimeZone.getTimeZone(tzId))
        .apply { set(2026, Calendar.NOVEMBER, 18, hour, minute, 0) }
        .timeInMillis

    val defaultVisual =
      NotificationThemeHelper.resolveVisual(AppThemeMode.DEFAULT, millisAt(12, 0), tzId)
    val dawnVisual =
      NotificationThemeHelper.resolveVisual(AppThemeMode.DYNAMIC_MOSQUE, millisAt(4, 30), tzId)
    val dayVisual =
      NotificationThemeHelper.resolveVisual(AppThemeMode.DYNAMIC_MOSQUE, millisAt(11, 0), tzId)
    val sunsetVisual =
      NotificationThemeHelper.resolveVisual(AppThemeMode.DYNAMIC_MOSQUE, millisAt(17, 30), tzId)
    val nightVisual =
      NotificationThemeHelper.resolveVisual(AppThemeMode.DYNAMIC_MOSQUE, millisAt(21, 0), tzId)

    assertNull(defaultVisual.mosqueDrawableRes)
    assertEquals(R.drawable.bg_widget_mosque_dawn, dawnVisual.mosqueDrawableRes)
    assertEquals(R.drawable.bg_widget_mosque_day, dayVisual.mosqueDrawableRes)
    assertEquals(R.drawable.bg_widget_mosque_sunset, sunsetVisual.mosqueDrawableRes)
    assertEquals(R.drawable.bg_widget_mosque_night, nightVisual.mosqueDrawableRes)
    assertNotEquals(dayVisual.accentColor, sunsetVisual.accentColor)
    assertNotEquals(dawnVisual.accentColor, nightVisual.accentColor)
  }

  @Test
  fun `renderLargeIconBitmap creates 96px circular bitmap for default and mosque themes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val defaultVisual = NotificationThemeHelper.resolveVisual(AppThemeMode.DEFAULT)
    val nightVisual = NotificationThemeHelper.resolveVisual(AppThemeMode.MOSQUE_NIGHT)

    val defaultIcon = NotificationThemeHelper.renderLargeIconBitmap(context, defaultVisual)
    val nightIcon = NotificationThemeHelper.renderLargeIconBitmap(context, nightVisual)

    assertNotNull(defaultIcon)
    assertEquals(96, defaultIcon!!.width)
    assertEquals(96, defaultIcon.height)

    assertNotNull(nightIcon)
    assertEquals(96, nightIcon!!.width)
    assertEquals(96, nightIcon.height)
  }

  @Test
  fun `ensurePreviewChannel and buildAndShowNotification build compact swipeable insistent banner`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val adzanChannelId =
      NotificationChannelManager.ensurePreviewChannel(
        context = app,
        category = SoundCategory.PRAYER_ENTRY,
        setting = SoundCategorySetting(mode = SoundMode.DEFAULT_ADZAN),
      )
    val vibrateChannelId =
      NotificationChannelManager.ensurePreviewChannel(
        context = app,
        category = SoundCategory.EXPIRY_REMINDER,
        setting = SoundCategorySetting(mode = SoundMode.VIBRATE_ONLY),
      )

    assertNotEquals(adzanChannelId, vibrateChannelId)
    assertNotNull(nm.getNotificationChannel(adzanChannelId))
    assertNotNull(nm.getNotificationChannel(vibrateChannelId))

    val posted =
      NotificationThemeHelper.buildAndShowNotification(
        context = app,
        notificationId = 99_901,
        channelId = adzanChannelId,
        title = "Dzuhur • 11:45",
        body = "Masuk waktu • Habis 15:01",
        isUrgent = true,
        appThemeMode = AppThemeMode.MOSQUE_SUNSET,
        timeZoneId = "Asia/Jakarta",
        prayerType = PrayerType.DZUHUR,
        isTestPreview = true,
        isInsistent = true,
      )
    assertTrue(posted)

    val shadowNm = Shadows.shadowOf(nm)
    val postedNotification = shadowNm.getNotification(99_901)
    assertNotNull(postedNotification)
    assertTrue((postedNotification!!.flags and Notification.FLAG_INSISTENT) != 0)
    assertEquals(0, postedNotification.flags and Notification.FLAG_ONGOING_EVENT)
    assertEquals(0, postedNotification.flags and Notification.FLAG_AUTO_CANCEL)
    assertNotNull(postedNotification.deleteIntent)
    assertEquals(2, postedNotification.actions.size)
  }

  @Test
  fun `ensureAlarmBannerChannel creates high importance silent banner channel bypassing dnd`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bannerChannelId = NotificationChannelManager.ensureAlarmBannerChannel(app)
    val channel = nm.getNotificationChannel(bannerChannelId)

    assertNotNull(channel)
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    assertNull(channel.sound)
    assertTrue(channel.canBypassDnd())
  }

  @Test
  fun `PrayerAlarmSoundService starts foreground insistent banner for reminders and stops cleanly on close`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val startIntent =
      Intent(app, PrayerAlarmSoundService::class.java).apply {
        action = PrayerAlarmSoundService.ACTION_START_ALARM_SOUND
        putExtra(PrayerAlarmSoundService.EXTRA_NOTIFICATION_ID, 77_001)
        putExtra(PrayerAlarmSoundService.EXTRA_TITLE, "Subuh • 15 mnt lagi!")
        putExtra(PrayerAlarmSoundService.EXTRA_BODY, "Segera shalat • Habis 05:35")
        putExtra(PrayerAlarmSoundService.EXTRA_IS_URGENT, true)
        putExtra(PrayerAlarmSoundService.EXTRA_APP_THEME_MODE, AppThemeMode.MOSQUE_NIGHT.id)
        putExtra(PrayerAlarmSoundService.EXTRA_TIMEZONE_ID, "Asia/Jakarta")
        putExtra(PrayerAlarmSoundService.EXTRA_PRAYER_TYPE, PrayerType.SUBUH.id)
        putExtra(PrayerAlarmSoundService.EXTRA_SOUND_MODE, SoundMode.DEFAULT_ADZAN.id)
        putExtra(PrayerAlarmSoundService.EXTRA_LOOP_AUDIO, true)
      }

    val controller = Robolectric.buildService(PrayerAlarmSoundService::class.java, startIntent).create()
    controller.startCommand(0, 0)

    val posted = Shadows.shadowOf(nm).getNotification(77_001)
    assertNotNull(posted)
    assertTrue((posted!!.flags and Notification.FLAG_INSISTENT) != 0)
    assertEquals(0, posted.flags and Notification.FLAG_ONGOING_EVENT)
    assertNotNull(posted.deleteIntent)

    val stopIntent =
      Intent(app, PrayerAlarmSoundService::class.java).apply {
        action = PrayerAlarmSoundService.ACTION_STOP_ALARM_SOUND
        putExtra(PrayerAlarmSoundService.EXTRA_NOTIFICATION_ID, 77_001)
      }
    Robolectric.buildService(PrayerAlarmSoundService::class.java, stopIntent).create().startCommand(0, 0)
    assertNull(Shadows.shadowOf(nm).getNotification(77_001))
  }

  @Test
  fun `PrayerAlarmSoundService plays obligatory prayer entry non-insistently until audio finishes`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val startIntent =
      Intent(app, PrayerAlarmSoundService::class.java).apply {
        action = PrayerAlarmSoundService.ACTION_START_ALARM_SOUND
        putExtra(PrayerAlarmSoundService.EXTRA_NOTIFICATION_ID, 77_002)
        putExtra(PrayerAlarmSoundService.EXTRA_TITLE, "Dzuhur • 11:45")
        putExtra(PrayerAlarmSoundService.EXTRA_BODY, "Masuk waktu • Habis 15:01")
        putExtra(PrayerAlarmSoundService.EXTRA_IS_URGENT, false)
        putExtra(PrayerAlarmSoundService.EXTRA_APP_THEME_MODE, AppThemeMode.MOSQUE_DAY.id)
        putExtra(PrayerAlarmSoundService.EXTRA_TIMEZONE_ID, "Asia/Jakarta")
        putExtra(PrayerAlarmSoundService.EXTRA_PRAYER_TYPE, PrayerType.DZUHUR.id)
        putExtra(PrayerAlarmSoundService.EXTRA_SOUND_MODE, SoundMode.DEFAULT_ADZAN.id)
        putExtra(PrayerAlarmSoundService.EXTRA_LOOP_AUDIO, false)
      }

    Robolectric.buildService(PrayerAlarmSoundService::class.java, startIntent)
      .create()
      .startCommand(0, 0)

    val posted = Shadows.shadowOf(nm).getNotification(77_002)
    assertNotNull(posted)
    assertEquals(0, posted!!.flags and Notification.FLAG_INSISTENT)
    assertEquals(0, posted.flags and Notification.FLAG_ONGOING_EVENT)
    assertNotNull(posted.deleteIntent)
  }

  @Test
  fun `receiver uses single full-duration playback for prayer entry and looping for H-15 reminder`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val receiver = PrayerAlarmReceiver()

    val entryIntent =
      Intent(app, PrayerAlarmReceiver::class.java).apply {
        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 66_001)
        putExtra(
          PrayerAlarmReceiver.EXTRA_CHANNEL_ID,
          NotificationChannelManager.channelIdFor(SoundCategory.PRAYER_ENTRY, 1),
        )
        putExtra(PrayerAlarmReceiver.EXTRA_TITLE, "Maghrib • 17:50")
        putExtra(PrayerAlarmReceiver.EXTRA_BODY, "Masuk waktu • Habis 19:02")
        putExtra(PrayerAlarmReceiver.EXTRA_IS_URGENT, false)
        putExtra(PrayerAlarmReceiver.EXTRA_ALARM_KIND, PrayerAlarmScheduler.AlarmKind.ENTRY.code)
        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.MAGHRIB.id)
        putExtra(PrayerAlarmReceiver.EXTRA_YEAR, 2026)
        putExtra(PrayerAlarmReceiver.EXTRA_MONTH, 11)
        putExtra(PrayerAlarmReceiver.EXTRA_DAY, 18)
      }

    receiver.onReceive(app, entryIntent)

    var entryServiceIntent: Intent? = null
    var attempts = 0
    while (entryServiceIntent == null && attempts < 30) {
      entryServiceIntent = Shadows.shadowOf(app).nextStartedService
      if (entryServiceIntent == null) Thread.sleep(25)
      attempts++
    }
    assertNotNull(entryServiceIntent)
    assertEquals(PrayerAlarmSoundService.ACTION_START_ALARM_SOUND, entryServiceIntent!!.action)
    assertEquals(false, entryServiceIntent.getBooleanExtra(PrayerAlarmSoundService.EXTRA_LOOP_AUDIO, true))

    val h15Intent =
      Intent(app, PrayerAlarmReceiver::class.java).apply {
        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 66_002)
        putExtra(
          PrayerAlarmReceiver.EXTRA_CHANNEL_ID,
          NotificationChannelManager.channelIdFor(SoundCategory.EXPIRY_REMINDER, 1),
        )
        putExtra(PrayerAlarmReceiver.EXTRA_TITLE, "Maghrib • 15 mnt lagi!")
        putExtra(PrayerAlarmReceiver.EXTRA_BODY, "Segera shalat • Habis 19:02")
        putExtra(PrayerAlarmReceiver.EXTRA_IS_URGENT, true)
        putExtra(
          PrayerAlarmReceiver.EXTRA_ALARM_KIND,
          PrayerAlarmScheduler.AlarmKind.REMINDER_H15.code,
        )
        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.MAGHRIB.id)
        putExtra(PrayerAlarmReceiver.EXTRA_YEAR, 2026)
        putExtra(PrayerAlarmReceiver.EXTRA_MONTH, 11)
        putExtra(PrayerAlarmReceiver.EXTRA_DAY, 18)
      }

    receiver.onReceive(app, h15Intent)

    var h15ServiceIntent: Intent? = null
    attempts = 0
    while (h15ServiceIntent == null && attempts < 30) {
      h15ServiceIntent = Shadows.shadowOf(app).nextStartedService
      if (h15ServiceIntent == null) Thread.sleep(25)
      attempts++
    }
    assertNotNull(h15ServiceIntent)
    assertEquals(true, h15ServiceIntent!!.getBooleanExtra(PrayerAlarmSoundService.EXTRA_LOOP_AUDIO, false))

    val stopBroadcast =
      Intent(PrayerAlarmReceiver.ACTION_STOP_ALARM).apply {
        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 66_001)
      }
    receiver.onReceive(app, stopBroadcast)

    val stopServiceIntent = Shadows.shadowOf(app).nextStartedService
    assertNotNull(stopServiceIntent)
    assertEquals(PrayerAlarmSoundService.ACTION_STOP_ALARM_SOUND, stopServiceIntent!!.action)
  }

  @Test
  fun `cached KemenagPrayerCalculator formatters format clock and Indonesian date accurately`() {
    val tzId = "Asia/Jakarta"
    val cal =
      Calendar.getInstance(TimeZone.getTimeZone(tzId)).apply {
        set(2026, Calendar.NOVEMBER, 18, 4, 25, 0)
        set(Calendar.MILLISECOND, 0)
      }
    assertEquals("04:25", KemenagPrayerCalculator.formatClock(cal.timeInMillis, tzId))
    val dateLabel = KemenagPrayerCalculator.formatIndonesianDate(cal.timeInMillis, tzId)
    assertTrue(dateLabel.contains("2026"))
    assertTrue(dateLabel.first().isUpperCase())
  }

  @Test
  fun `receiver ACTION_MARK_ALREADY_PRAYED marks checklist in DataStore`() {
    kotlinx.coroutines.runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val repo = PrayerPreferencesRepository(context)
      repo.setAlreadyPrayed(PrayerType.ASHAR, false, "2026-11-18")

      val intent =
        Intent(PrayerAlarmReceiver.ACTION_MARK_ALREADY_PRAYED).apply {
          putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 55_001)
          putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.ASHAR.id)
          putExtra(PrayerAlarmReceiver.EXTRA_YEAR, 2026)
          putExtra(PrayerAlarmReceiver.EXTRA_MONTH, 11)
          putExtra(PrayerAlarmReceiver.EXTRA_DAY, 18)
        }
      PrayerAlarmReceiver().onReceive(context, intent)

      var updated = repo.getCurrent()
      var attempts = 0
      while (!updated.isCheckedToday(PrayerType.ASHAR, "2026-11-18") && attempts < 30) {
        kotlinx.coroutines.delay(50)
        updated = repo.getCurrent()
        attempts++
      }
      assertTrue(updated.isCheckedToday(PrayerType.ASHAR, "2026-11-18"))
    }
  }
}
