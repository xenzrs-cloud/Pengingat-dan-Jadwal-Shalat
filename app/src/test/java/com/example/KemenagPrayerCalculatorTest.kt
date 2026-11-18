package com.example

import com.example.domain.IndonesianCities
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KemenagPrayerCalculatorTest {

  @Test
  fun imsakIsExactlyTenMinutesBeforeSubuh() {
    val jakarta = IndonesianCities.jakarta
    val schedule =
      KemenagPrayerCalculator.calculateForDate(
        year = 2026,
        month = 11,
        day = 18,
        latitude = jakarta.latitude,
        longitude = jakarta.longitude,
        timeZoneId = jakarta.timeZoneId,
        locationName = jakarta.name,
      )

    val imsak = schedule.slotOf(PrayerType.IMSAK).startEpochMillis
    val subuh = schedule.slotOf(PrayerType.SUBUH).startEpochMillis
    assertEquals(10 * 60_000L, subuh - imsak)
  }

  @Test
  fun dailyPrayerTimesAreStrictlyOrderedAndEndBoundariesMatch() {
    val jakarta = IndonesianCities.jakarta
    val schedule =
      KemenagPrayerCalculator.calculateForDate(
        year = 2026,
        month = 3,
        day = 15,
        latitude = jakarta.latitude,
        longitude = jakarta.longitude,
        timeZoneId = jakarta.timeZoneId,
        locationName = jakarta.name,
      )

    val imsak = schedule.slotOf(PrayerType.IMSAK)
    val subuh = schedule.slotOf(PrayerType.SUBUH)
    val terbit = schedule.slotOf(PrayerType.TERBIT)
    val dzuhur = schedule.slotOf(PrayerType.DZUHUR)
    val ashar = schedule.slotOf(PrayerType.ASHAR)
    val maghrib = schedule.slotOf(PrayerType.MAGHRIB)
    val isya = schedule.slotOf(PrayerType.ISYA)

    assertTrue(imsak.startEpochMillis < subuh.startEpochMillis)
    assertTrue(subuh.startEpochMillis < terbit.startEpochMillis)
    assertTrue(terbit.startEpochMillis < dzuhur.startEpochMillis)
    assertTrue(dzuhur.startEpochMillis < ashar.startEpochMillis)
    assertTrue(ashar.startEpochMillis < maghrib.startEpochMillis)
    assertTrue(maghrib.startEpochMillis < isya.startEpochMillis)

    // End boundaries
    assertEquals(terbit.startEpochMillis, subuh.endEpochMillis)
    assertEquals(ashar.startEpochMillis, dzuhur.endEpochMillis)
    assertEquals(maghrib.startEpochMillis, ashar.endEpochMillis)
    assertEquals(isya.startEpochMillis, maghrib.endEpochMillis)
    assertTrue((isya.endEpochMillis ?: 0L) > isya.startEpochMillis)
  }

  @Test
  fun formatCountdownProducesTabularHhMmSs() {
    val twoHoursFiveMinSevenSec = (2 * 3600L + 5 * 60L + 7L) * 1000L
    assertEquals("02:05:07", KemenagPrayerCalculator.formatCountdown(twoHoursFiveMinSevenSec))
  }
}
