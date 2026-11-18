package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.NotificationChannelManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.SoundCategory
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
}
