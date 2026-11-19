package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline prayer time calculator using Kemenag RI / Singapore standard parameters:
 * - Fajr angle: 20.0 degrees
 * - Isha angle: 18.0 degrees
 * - Madhab: Shafi'i (shadow factor = 1)
 * - Imsak: exactly 10 minutes before Subuh (Fajr)
 */
object KemenagPrayerCalculator {

  private const val FAJR_ANGLE = 20.0
  private const val ISHA_ANGLE = 18.0
  private const val SUNRISE_ALTITUDE = -0.8333 // Standard solar disk + atmospheric refraction
  private const val IMSAK_OFFSET_MINUTES = 10

  private val idLocale = Locale.forLanguageTag("id-ID")
  private val tzCache = ConcurrentHashMap<String, TimeZone>()
  private val clockFormatterTl =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", idLocale)
    }
  private val indonesianDateFormatterTl =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("EEEE, d MMMM yyyy", idLocale)
    }

  private fun cachedTimeZone(timeZoneId: String): TimeZone =
    tzCache.getOrPut(timeZoneId) { TimeZone.getTimeZone(timeZoneId) }

  fun calculateForDate(
    year: Int,
    month: Int, // 1..12
    day: Int,
    latitude: Double,
    longitude: Double,
    timeZoneId: String,
    locationName: String,
  ): DailyPrayerSchedule {
    val tz = cachedTimeZone(timeZoneId)
    val baseCalendar =
      Calendar.getInstance(tz).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

    val rawToday = computeRawTimes(year, month, day, latitude, longitude, tz)

    // Compute tomorrow's Subuh so Isya's end boundary is tomorrow's Subuh
    val tomorrowCal =
      (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    val rawTomorrow =
      computeRawTimes(
        tomorrowCal.get(Calendar.YEAR),
        tomorrowCal.get(Calendar.MONTH) + 1,
        tomorrowCal.get(Calendar.DAY_OF_MONTH),
        latitude,
        longitude,
        tz,
      )

    val subuhMillis = toEpochMillis(baseCalendar, rawToday.fajrHours)
    val imsakMillis = subuhMillis - IMSAK_OFFSET_MINUTES * 60_000L
    val terbitMillis = toEpochMillis(baseCalendar, rawToday.sunriseHours)
    val dzuhurMillis = toEpochMillis(baseCalendar, rawToday.dhuhrHours)
    val asharMillis = toEpochMillis(baseCalendar, rawToday.asrHours)
    val maghribMillis = toEpochMillis(baseCalendar, rawToday.maghribHours)
    val isyaMillis = toEpochMillis(baseCalendar, rawToday.ishaHours)
    val tomorrowSubuhMillis = toEpochMillis(tomorrowCal, rawTomorrow.fajrHours)

    val slots =
      mapOf(
        PrayerType.IMSAK to PrayerSlot(PrayerType.IMSAK, imsakMillis, subuhMillis, timeZoneId),
        PrayerType.SUBUH to PrayerSlot(PrayerType.SUBUH, subuhMillis, terbitMillis, timeZoneId),
        PrayerType.TERBIT to PrayerSlot(PrayerType.TERBIT, terbitMillis, null, timeZoneId),
        PrayerType.DZUHUR to PrayerSlot(PrayerType.DZUHUR, dzuhurMillis, asharMillis, timeZoneId),
        PrayerType.ASHAR to PrayerSlot(PrayerType.ASHAR, asharMillis, maghribMillis, timeZoneId),
        PrayerType.MAGHRIB to PrayerSlot(PrayerType.MAGHRIB, maghribMillis, isyaMillis, timeZoneId),
        PrayerType.ISYA to
          PrayerSlot(PrayerType.ISYA, isyaMillis, tomorrowSubuhMillis, timeZoneId),
      )

    val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    return DailyPrayerSchedule(
      dateKey = dateKey,
      year = year,
      month = month,
      day = day,
      latitude = latitude,
      longitude = longitude,
      timeZoneId = timeZoneId,
      locationName = locationName,
      slots = slots,
    )
  }

  fun calculateTodayAndTomorrow(
    nowMillis: Long,
    latitude: Double,
    longitude: Double,
    timeZoneId: String,
    locationName: String,
  ): Pair<DailyPrayerSchedule, DailyPrayerSchedule> {
    val tz = cachedTimeZone(timeZoneId)
    val todayCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
    val today =
      calculateForDate(
        year = todayCal.get(Calendar.YEAR),
        month = todayCal.get(Calendar.MONTH) + 1,
        day = todayCal.get(Calendar.DAY_OF_MONTH),
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        locationName = locationName,
      )

    val tomorrowCal =
      (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    val tomorrow =
      calculateForDate(
        year = tomorrowCal.get(Calendar.YEAR),
        month = tomorrowCal.get(Calendar.MONTH) + 1,
        day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        locationName = locationName,
      )

    return today to tomorrow
  }

  fun dateKeyForMillis(epochMillis: Long, timeZoneId: String): String {
    val tz = cachedTimeZone(timeZoneId)
    val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun formatClock(epochMillis: Long, timeZoneId: String): String {
    val formatter = clockFormatterTl.get() ?: SimpleDateFormat("HH:mm", idLocale)
    formatter.timeZone = cachedTimeZone(timeZoneId)
    return formatter.format(Date(epochMillis))
  }

  fun formatIndonesianDate(epochMillis: Long, timeZoneId: String): String {
    val formatter =
      indonesianDateFormatterTl.get() ?: SimpleDateFormat("EEEE, d MMMM yyyy", idLocale)
    formatter.timeZone = cachedTimeZone(timeZoneId)
    val raw = formatter.format(Date(epochMillis))
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(idLocale) else it.toString() }
  }

  fun formatCountdown(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  }

  private data class RawPrayerHours(
    val fajrHours: Double,
    val sunriseHours: Double,
    val dhuhrHours: Double,
    val asrHours: Double,
    val maghribHours: Double,
    val ishaHours: Double,
  )

  private fun computeRawTimes(
    year: Int,
    month: Int,
    day: Int,
    latitude: Double,
    longitude: Double,
    tz: TimeZone,
  ): RawPrayerHours {
    val cal =
      Calendar.getInstance(tz).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val tzOffsetHours = tz.getOffset(cal.timeInMillis) / 3_600_000.0
    val jd0 = julianDay(year, month, day) - longitude / 360.0

    // Two-pass iterative solar refinement for high precision
    val transit = approximateTransit(jd0, longitude, tzOffsetHours)
    val fajr =
      refineSunAngleTime(jd0, latitude, longitude, tzOffsetHours, -FAJR_ANGLE, isRising = true)
    val sunrise =
      refineSunAngleTime(
        jd0,
        latitude,
        longitude,
        tzOffsetHours,
        SUNRISE_ALTITUDE,
        isRising = true,
      )
    val dhuhr = refineTransit(jd0, longitude, tzOffsetHours, transit)
    val asr = refineAsrTime(jd0, latitude, longitude, tzOffsetHours, shadowFactor = 1.0)
    val maghrib =
      refineSunAngleTime(
        jd0,
        latitude,
        longitude,
        tzOffsetHours,
        SUNRISE_ALTITUDE,
        isRising = false,
      )
    val isha =
      refineSunAngleTime(jd0, latitude, longitude, tzOffsetHours, -ISHA_ANGLE, isRising = false)

    return RawPrayerHours(
      fajrHours = fajr,
      sunriseHours = sunrise,
      dhuhrHours = dhuhr,
      asrHours = asr,
      maghribHours = maghrib,
      ishaHours = isha,
    )
  }

  private fun julianDay(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = floor(y / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
  }

  private data class SolarPosition(val declinationRad: Double, val equationOfTimeHours: Double)

  private fun solarPosition(jd: Double): SolarPosition {
    val d = jd - 2451545.0
    val g = normalizeDegrees(357.529 + 0.98560028 * d)
    val q = normalizeDegrees(280.459 + 0.98564736 * d)
    val l = normalizeDegrees(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2.0 * g))
    val e = 23.439 - 0.00000036 * d

    val raRad = atan2(cosDeg(e) * sinDeg(l), cosDeg(l))
    val raHours = normalizeHours(Math.toDegrees(raRad) / 15.0)
    val declRad = asin(sinDeg(e) * sinDeg(l))
    val eqtHours = q / 15.0 - raHours
    val normalizedEqt =
      when {
        eqtHours > 12.0 -> eqtHours - 24.0
        eqtHours < -12.0 -> eqtHours + 24.0
        else -> eqtHours
      }
    return SolarPosition(declinationRad = declRad, equationOfTimeHours = normalizedEqt)
  }

  private fun approximateTransit(jd0: Double, longitude: Double, tzOffsetHours: Double): Double {
    val solar = solarPosition(jd0 + 0.5)
    return 12.0 + tzOffsetHours - longitude / 15.0 - solar.equationOfTimeHours
  }

  private fun refineTransit(
    jd0: Double,
    longitude: Double,
    tzOffsetHours: Double,
    initialHours: Double,
  ): Double {
    var t = initialHours
    repeat(2) {
      val jd = jd0 + (t - tzOffsetHours) / 24.0
      val solar = solarPosition(jd)
      t = 12.0 + tzOffsetHours - longitude / 15.0 - solar.equationOfTimeHours
    }
    return t
  }

  private fun refineSunAngleTime(
    jd0: Double,
    latitude: Double,
    longitude: Double,
    tzOffsetHours: Double,
    altitudeDeg: Double,
    isRising: Boolean,
  ): Double {
    var t = if (isRising) 6.0 else 18.0
    repeat(3) {
      val jd = jd0 + (t - tzOffsetHours) / 24.0
      val solar = solarPosition(jd)
      val transit = 12.0 + tzOffsetHours - longitude / 15.0 - solar.equationOfTimeHours
      val hourAngleHours = hourAngleForAltitude(latitude, solar.declinationRad, altitudeDeg)
      t = if (isRising) transit - hourAngleHours else transit + hourAngleHours
    }
    return t
  }

  private fun refineAsrTime(
    jd0: Double,
    latitude: Double,
    longitude: Double,
    tzOffsetHours: Double,
    shadowFactor: Double,
  ): Double {
    var t = 15.0
    repeat(3) {
      val jd = jd0 + (t - tzOffsetHours) / 24.0
      val solar = solarPosition(jd)
      val transit = 12.0 + tzOffsetHours - longitude / 15.0 - solar.equationOfTimeHours
      val latRad = Math.toRadians(latitude)
      val cotTerm = shadowFactor + tan(kotlin.math.abs(latRad - solar.declinationRad))
      val altitudeRad = atan(1.0 / cotTerm)
      val altitudeDeg = Math.toDegrees(altitudeRad)
      val hourAngleHours = hourAngleForAltitude(latitude, solar.declinationRad, altitudeDeg)
      t = transit + hourAngleHours
    }
    return t
  }

  private fun hourAngleForAltitude(
    latitude: Double,
    declinationRad: Double,
    altitudeDeg: Double,
  ): Double {
    val latRad = Math.toRadians(latitude)
    val altRad = Math.toRadians(altitudeDeg)
    val cosH =
      ((sin(altRad) - sin(latRad) * sin(declinationRad)) / (cos(latRad) * cos(declinationRad)))
        .coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cosH)) / 15.0
  }

  private fun toEpochMillis(midnightCalendar: Calendar, hoursFromMidnight: Double): Long {
    val totalMinutes = (hoursFromMidnight * 60.0).roundToInt()
    return (midnightCalendar.clone() as Calendar)
      .apply { add(Calendar.MINUTE, totalMinutes) }
      .timeInMillis
  }

  private fun normalizeDegrees(deg: Double): Double {
    var d = deg % 360.0
    if (d < 0) d += 360.0
    return d
  }

  private fun normalizeHours(hours: Double): Double {
    var h = hours % 24.0
    if (h < 0) h += 24.0
    return h
  }

  private fun sinDeg(deg: Double): Double = sin(deg * PI / 180.0)

  private fun cosDeg(deg: Double): Double = cos(deg * PI / 180.0)
}
