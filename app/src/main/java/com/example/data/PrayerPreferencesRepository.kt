package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppThemeMode
import com.example.domain.IndonesianCities
import com.example.domain.IndonesianCity
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerType
import com.example.domain.QuickSoundOverride
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode
import com.example.domain.WidgetThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prayerDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "prayer_preferences")

class PrayerPreferencesRepository(private val context: Context) {

  private object Keys {
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
    val TIMEZONE_ID = stringPreferencesKey("timezone_id")
    val LOCATION_NAME = stringPreferencesKey("location_name")
    val IS_GPS_LOCATION = booleanPreferencesKey("is_gps_location")
    val CHANNEL_VERSION = intPreferencesKey("channel_version")
    val CHECKED_DATE_KEY = stringPreferencesKey("checked_date_key")
    val CHECKED_PRAYERS = stringSetPreferencesKey("checked_prayers")
    val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    val WIDGET_THEME_MODE = stringPreferencesKey("widget_theme_mode")

    fun modeKey(category: SoundCategory) = stringPreferencesKey("sound_mode_${category.id}")

    fun customFileKey(category: SoundCategory) =
      stringPreferencesKey("sound_custom_file_${category.id}")

    fun customDisplayKey(category: SoundCategory) =
      stringPreferencesKey("sound_custom_display_${category.id}")

    fun quickOverrideKey(prayerType: PrayerType) =
      stringPreferencesKey("quick_override_${prayerType.id}")
  }

  val preferencesFlow: Flow<PrayerPreferences> =
    context.prayerDataStore.data.map { prefs -> mapPreferences(prefs) }

  suspend fun getCurrent(): PrayerPreferences = preferencesFlow.first()

  suspend fun updateLocation(
    latitude: Double,
    longitude: Double,
    timeZoneId: String,
    locationName: String,
    isGps: Boolean,
  ) {
    context.prayerDataStore.edit { prefs ->
      prefs[Keys.LATITUDE] = latitude
      prefs[Keys.LONGITUDE] = longitude
      prefs[Keys.TIMEZONE_ID] = timeZoneId
      prefs[Keys.LOCATION_NAME] = locationName
      prefs[Keys.IS_GPS_LOCATION] = isGps
    }
  }

  suspend fun selectCity(city: IndonesianCity) {
    updateLocation(
      latitude = city.latitude,
      longitude = city.longitude,
      timeZoneId = city.timeZoneId,
      locationName = "${city.name}, ${city.province}",
      isGps = false,
    )
  }

  suspend fun setAlreadyPrayed(prayerType: PrayerType, checked: Boolean, todayDateKey: String) {
    context.prayerDataStore.edit { prefs ->
      val currentStoredDate = prefs[Keys.CHECKED_DATE_KEY] ?: ""
      val currentSet =
        if (currentStoredDate == todayDateKey) {
          (prefs[Keys.CHECKED_PRAYERS] ?: emptySet()).toMutableSet()
        } else {
          mutableSetOf()
        }
      if (checked) {
        currentSet.add(prayerType.id)
      } else {
        currentSet.remove(prayerType.id)
      }
      prefs[Keys.CHECKED_DATE_KEY] = todayDateKey
      prefs[Keys.CHECKED_PRAYERS] = currentSet
    }
  }

  suspend fun resetCheckedPrayersIfDateChanged(todayDateKey: String) {
    context.prayerDataStore.edit { prefs ->
      val currentStoredDate = prefs[Keys.CHECKED_DATE_KEY] ?: ""
      if (currentStoredDate != todayDateKey) {
        prefs[Keys.CHECKED_DATE_KEY] = todayDateKey
        prefs[Keys.CHECKED_PRAYERS] = emptySet()
      }
    }
  }

  suspend fun setQuickOverride(prayerType: PrayerType, override: QuickSoundOverride) {
    context.prayerDataStore.edit { prefs ->
      prefs[Keys.quickOverrideKey(prayerType)] = override.id
    }
  }

  suspend fun setAppThemeMode(mode: AppThemeMode) {
    context.prayerDataStore.edit { prefs ->
      prefs[Keys.APP_THEME_MODE] = mode.id
    }
  }

  suspend fun setWidgetThemeMode(mode: WidgetThemeMode) {
    context.prayerDataStore.edit { prefs ->
      prefs[Keys.WIDGET_THEME_MODE] = mode.id
    }
  }

  suspend fun saveSoundSettingsAndBumpChannelVersion(
    prayerEntry: SoundCategorySetting,
    imsakEntry: SoundCategorySetting,
    expiryReminder: SoundCategorySetting,
  ): Pair<Int, Int> {
    var oldVersion = 1
    var newVersion = 2
    context.prayerDataStore.edit { prefs ->
      oldVersion = prefs[Keys.CHANNEL_VERSION] ?: 1
      newVersion = oldVersion + 1
      prefs[Keys.CHANNEL_VERSION] = newVersion

      writeCategory(prefs, SoundCategory.PRAYER_ENTRY, prayerEntry)
      writeCategory(prefs, SoundCategory.IMSAK_ENTRY, imsakEntry)
      writeCategory(prefs, SoundCategory.EXPIRY_REMINDER, expiryReminder)
    }
    return oldVersion to newVersion
  }

  private fun writeCategory(
    prefs: androidx.datastore.preferences.core.MutablePreferences,
    category: SoundCategory,
    setting: SoundCategorySetting,
  ) {
    prefs[Keys.modeKey(category)] = setting.mode.id
    if (setting.customFileName != null) {
      prefs[Keys.customFileKey(category)] = setting.customFileName
    } else {
      prefs.remove(Keys.customFileKey(category))
    }
    if (setting.customDisplayName != null) {
      prefs[Keys.customDisplayKey(category)] = setting.customDisplayName
    } else {
      prefs.remove(Keys.customDisplayKey(category))
    }
  }

  private fun mapPreferences(prefs: Preferences): PrayerPreferences {
    val overrides =
      PrayerType.overridableSlots.associateWith { type ->
        QuickSoundOverride.fromId(prefs[Keys.quickOverrideKey(type)])
      }
    val checkedIds = prefs[Keys.CHECKED_PRAYERS] ?: emptySet()
    val checkedPrayers = checkedIds.mapNotNull { PrayerType.fromId(it) }.toSet()

    return PrayerPreferences(
      latitude = prefs[Keys.LATITUDE] ?: IndonesianCities.jakarta.latitude,
      longitude = prefs[Keys.LONGITUDE] ?: IndonesianCities.jakarta.longitude,
      timeZoneId = prefs[Keys.TIMEZONE_ID] ?: IndonesianCities.jakarta.timeZoneId,
      locationName = prefs[Keys.LOCATION_NAME] ?: "Jakarta (Default)",
      isGpsLocation = prefs[Keys.IS_GPS_LOCATION] ?: false,
      prayerEntrySound = readCategory(prefs, SoundCategory.PRAYER_ENTRY),
      imsakEntrySound = readCategory(prefs, SoundCategory.IMSAK_ENTRY),
      expiryReminderSound = readCategory(prefs, SoundCategory.EXPIRY_REMINDER),
      quickOverrides = overrides,
      checkedDateKey = prefs[Keys.CHECKED_DATE_KEY] ?: "",
      checkedPrayers = checkedPrayers,
      channelVersion = prefs[Keys.CHANNEL_VERSION] ?: 1,
      appThemeMode = AppThemeMode.fromId(prefs[Keys.APP_THEME_MODE]),
      widgetThemeMode = WidgetThemeMode.fromId(prefs[Keys.WIDGET_THEME_MODE]),
    )
  }

  private fun readCategory(prefs: Preferences, category: SoundCategory): SoundCategorySetting {
    return SoundCategorySetting(
      mode = SoundMode.fromId(prefs[Keys.modeKey(category)]),
      customFileName = prefs[Keys.customFileKey(category)],
      customDisplayName = prefs[Keys.customDisplayKey(category)],
    )
  }
}
