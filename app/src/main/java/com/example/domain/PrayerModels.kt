package com.example.domain

enum class PrayerType(
  val id: String,
  val displayName: String,
  val isObligatory: Boolean,
) {
  IMSAK("imsak", "Imsak", false),
  SUBUH("subuh", "Subuh", true),
  TERBIT("terbit", "Terbit (Syuruq)", false),
  DZUHUR("dzuhur", "Dzuhur", true),
  ASHAR("ashar", "Ashar", true),
  MAGHRIB("maghrib", "Maghrib", true),
  ISYA("isya", "Isya", true);

  companion object {
    val obligatoryPrayers: List<PrayerType> = listOf(SUBUH, DZUHUR, ASHAR, MAGHRIB, ISYA)
    val overridableSlots: List<PrayerType> = listOf(IMSAK, SUBUH, DZUHUR, ASHAR, MAGHRIB, ISYA)

    fun fromId(id: String): PrayerType? = entries.firstOrNull { it.id == id }
  }
}

enum class SoundCategory(
  val id: String,
  val title: String,
  val subtitle: String,
) {
  PRAYER_ENTRY(
    id = "prayer_entry",
    title = "(A) Alarm Masuk Waktu Shalat",
    subtitle = "Berbunyi tepat saat waktu Subuh, Dzuhur, Ashar, Maghrib, dan Isya tiba.",
  ),
  IMSAK_ENTRY(
    id = "imsak_entry",
    title = "(B) Alarm Waktu Imsak",
    subtitle = "Pengingat sahur tepat 10 menit sebelum masuk waktu Subuh.",
  ),
  EXPIRY_REMINDER(
    id = "expiry_reminder",
    title = "(C) Pengingat Sebelum Waktu Habis (H-30 & H-15)",
    subtitle = "Pengingat 30 menit dan peringatan tegas 15 menit sebelum waktu shalat habis.",
  ),
}

enum class SoundMode(val id: String, val label: String, val description: String) {
  DEFAULT_ADZAN(
    id = "default_adzan",
    label = "Adzan / Suara Bawaan",
    description = "Memutar nada adzan harmonis bawaan aplikasi",
  ),
  CUSTOM_FILE(
    id = "custom_file",
    label = "Suara Lokal (File Pribadi)",
    description = "Memutar file audio pilihan dari penyimpanan HP",
  ),
  VIBRATE_ONLY(
    id = "vibrate_only",
    label = "Getar Saja",
    description = "Notifikasi tanpa suara dengan getaran",
  ),
  SILENT(
    id = "silent",
    label = "Hening / Diam",
    description = "Notifikasi visual senyap tanpa suara dan getar",
  );

  companion object {
    fun fromId(id: String?): SoundMode = entries.firstOrNull { it.id == id } ?: DEFAULT_ADZAN
  }
}

enum class QuickSoundOverride(val id: String, val label: String) {
  DEFAULT("default", "Default"),
  VIBRATE("vibrate", "Getar"),
  SILENT("silent", "Hening");

  companion object {
    fun fromId(id: String?): QuickSoundOverride = entries.firstOrNull { it.id == id } ?: DEFAULT
  }
}

data class SoundCategorySetting(
  val mode: SoundMode = SoundMode.DEFAULT_ADZAN,
  val customFileName: String? = null,
  val customDisplayName: String? = null,
)

data class IndonesianCity(
  val name: String,
  val province: String,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
)

object IndonesianCities {
  val jakarta = IndonesianCity("Jakarta", "DKI Jakarta", -6.2088, 106.8456, "Asia/Jakarta")

  val all: List<IndonesianCity> =
    listOf(
      jakarta,
      IndonesianCity("Bandung", "Jawa Barat", -6.9175, 107.6191, "Asia/Jakarta"),
      IndonesianCity("Surabaya", "Jawa Timur", -7.2575, 112.7521, "Asia/Jakarta"),
      IndonesianCity("Yogyakarta", "DI Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
      IndonesianCity("Semarang", "Jawa Tengah", -6.9667, 110.4167, "Asia/Jakarta"),
      IndonesianCity("Bekasi", "Jawa Barat", -6.2383, 106.9756, "Asia/Jakarta"),
      IndonesianCity("Bogor", "Jawa Barat", -6.5971, 106.8060, "Asia/Jakarta"),
      IndonesianCity("Malang", "Jawa Timur", -7.9666, 112.6326, "Asia/Jakarta"),
      IndonesianCity("Medan", "Sumatera Utara", 3.5952, 98.6722, "Asia/Jakarta"),
      IndonesianCity("Palembang", "Sumatera Selatan", -2.9761, 104.7754, "Asia/Jakarta"),
      IndonesianCity("Padang", "Sumatera Barat", -0.9471, 100.4172, "Asia/Jakarta"),
      IndonesianCity("Pekanbaru", "Riau", 0.5071, 101.4478, "Asia/Jakarta"),
      IndonesianCity("Banda Aceh", "Aceh", 5.5483, 95.3238, "Asia/Jakarta"),
      IndonesianCity("Bandar Lampung", "Lampung", -5.3971, 105.2668, "Asia/Jakarta"),
      IndonesianCity("Makassar", "Sulawesi Selatan", -5.1477, 119.4327, "Asia/Makassar"),
      IndonesianCity("Denpasar", "Bali", -8.6705, 115.2126, "Asia/Makassar"),
      IndonesianCity("Balikpapan", "Kalimantan Timur", -1.2379, 116.8529, "Asia/Makassar"),
      IndonesianCity("Banjarmasin", "Kalimantan Selatan", -3.3194, 114.5908, "Asia/Makassar"),
      IndonesianCity("Pontianak", "Kalimantan Barat", -0.0263, 109.3425, "Asia/Jakarta"),
      IndonesianCity("Manado", "Sulawesi Utara", 1.4748, 124.8421, "Asia/Makassar"),
      IndonesianCity("Mataram", "Nusa Tenggara Barat", -8.5833, 116.1167, "Asia/Makassar"),
      IndonesianCity("Ambon", "Maluku", -3.6954, 128.1814, "Asia/Jayapura"),
      IndonesianCity("Jayapura", "Papua", -2.5916, 140.6690, "Asia/Jayapura"),
    )

  fun inferTimeZoneId(longitude: Double): String =
    when {
      longitude < 114.0 -> "Asia/Jakarta"
      longitude < 126.5 -> "Asia/Makassar"
      else -> "Asia/Jayapura"
    }

  fun findNearestCityName(latitude: Double, longitude: Double): IndonesianCity? {
    return all.minByOrNull { city ->
      val dLat = city.latitude - latitude
      val dLon = city.longitude - longitude
      dLat * dLat + dLon * dLon
    }
  }
}

data class PrayerSlot(
  val type: PrayerType,
  val startEpochMillis: Long,
  val endEpochMillis: Long?,
  val timeZoneId: String,
)

data class DailyPrayerSchedule(
  val dateKey: String,
  val year: Int,
  val month: Int,
  val day: Int,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val locationName: String,
  val slots: Map<PrayerType, PrayerSlot>,
) {
  fun slotOf(type: PrayerType): PrayerSlot = slots.getValue(type)
}

data class PrayerPreferences(
  val latitude: Double = IndonesianCities.jakarta.latitude,
  val longitude: Double = IndonesianCities.jakarta.longitude,
  val timeZoneId: String = IndonesianCities.jakarta.timeZoneId,
  val locationName: String = "Jakarta (Default)",
  val isGpsLocation: Boolean = false,
  val prayerEntrySound: SoundCategorySetting = SoundCategorySetting(),
  val imsakEntrySound: SoundCategorySetting = SoundCategorySetting(),
  val expiryReminderSound: SoundCategorySetting = SoundCategorySetting(),
  val quickOverrides: Map<PrayerType, QuickSoundOverride> = emptyMap(),
  val checkedDateKey: String = "",
  val checkedPrayers: Set<PrayerType> = emptySet(),
  val channelVersion: Int = 1,
) {
  fun settingFor(category: SoundCategory): SoundCategorySetting =
    when (category) {
      SoundCategory.PRAYER_ENTRY -> prayerEntrySound
      SoundCategory.IMSAK_ENTRY -> imsakEntrySound
      SoundCategory.EXPIRY_REMINDER -> expiryReminderSound
    }

  fun overrideFor(prayerType: PrayerType): QuickSoundOverride =
    quickOverrides[prayerType] ?: QuickSoundOverride.DEFAULT

  fun isCheckedToday(prayerType: PrayerType, todayKey: String): Boolean =
    checkedDateKey == todayKey && checkedPrayers.contains(prayerType)
}
