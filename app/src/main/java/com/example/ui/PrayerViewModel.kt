package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.NotificationChannelManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.audio.AudioPreviewPlayer
import com.example.data.CustomAudioStorage
import com.example.data.LocationHelper
import com.example.data.PrayerPreferencesRepository
import com.example.domain.DailyPrayerSchedule
import com.example.domain.IndonesianCity
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerSlot
import com.example.domain.PrayerType
import com.example.domain.QuickSoundOverride
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NextEventUi(
  val type: PrayerType,
  val targetMillis: Long,
  val remainingMillis: Long,
  val isTomorrow: Boolean,
)

data class ActivePrayerUi(
  val type: PrayerType,
  val startMillis: Long,
  val endMillis: Long,
  val remainingMillis: Long,
)

data class PrayerUiState(
  val nowMillis: Long = System.currentTimeMillis(),
  val todaySchedule: DailyPrayerSchedule? = null,
  val tomorrowSchedule: DailyPrayerSchedule? = null,
  val nextEvent: NextEventUi? = null,
  val activePrayer: ActivePrayerUi? = null,
  val preferences: PrayerPreferences = PrayerPreferences(),
  val draftPrayerEntry: SoundCategorySetting = SoundCategorySetting(),
  val draftImsakEntry: SoundCategorySetting = SoundCategorySetting(),
  val draftExpiryReminder: SoundCategorySetting = SoundCategorySetting(),
  val playingPreviewCategory: SoundCategory? = null,
  val isLoadingLocation: Boolean = false,
  val canScheduleExactAlarms: Boolean = true,
  val snackbarMessage: String? = null,
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val repository = PrayerPreferencesRepository(appContext)
  private val audioPreviewPlayer = AudioPreviewPlayer(appContext)

  private val _uiState =
    MutableStateFlow(
      PrayerUiState(canScheduleExactAlarms = PrayerAlarmScheduler.canScheduleExactAlarms(appContext))
    )
  val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

  private var lastComputedDateKey: String = ""
  private var draftInitialized = false

  init {
    observePreferences()
    observeAudioPreview()
    startCountdownTicker()
  }

  private fun observePreferences() {
    viewModelScope.launch {
      repository.preferencesFlow.collect { prefs ->
        val now = System.currentTimeMillis()
        val todayKey = KemenagPrayerCalculator.dateKeyForMillis(now, prefs.timeZoneId)
        if (prefs.checkedDateKey.isNotEmpty() && prefs.checkedDateKey != todayKey) {
          repository.resetCheckedPrayersIfDateChanged(todayKey)
        }
        _uiState.update { state ->
          val updated =
            state.copy(
              preferences = prefs,
              draftPrayerEntry = if (!draftInitialized) prefs.prayerEntrySound else state.draftPrayerEntry,
              draftImsakEntry = if (!draftInitialized) prefs.imsakEntrySound else state.draftImsakEntry,
              draftExpiryReminder =
                if (!draftInitialized) prefs.expiryReminderSound else state.draftExpiryReminder,
            )
          draftInitialized = true
          recalculateDerivedState(updated, now)
        }
        withContext(Dispatchers.Default) {
          PrayerAlarmScheduler.rescheduleAll(appContext, prefs, now)
        }
      }
    }
  }

  private fun observeAudioPreview() {
    viewModelScope.launch {
      audioPreviewPlayer.playingCategory.collect { category ->
        _uiState.update { it.copy(playingPreviewCategory = category) }
      }
    }
  }

  private fun startCountdownTicker() {
    viewModelScope.launch {
      while (isActive) {
        val now = System.currentTimeMillis()
        val currentState = _uiState.value
        val tzId = currentState.preferences.timeZoneId
        val currentDateKey = KemenagPrayerCalculator.dateKeyForMillis(now, tzId)

        if (lastComputedDateKey.isNotEmpty() && lastComputedDateKey != currentDateKey) {
          repository.resetCheckedPrayersIfDateChanged(currentDateKey)
          withContext(Dispatchers.Default) {
            PrayerAlarmScheduler.rescheduleAll(appContext, currentState.preferences, now)
          }
        }
        lastComputedDateKey = currentDateKey

        _uiState.update { state -> recalculateDerivedState(state, now) }
        delay(1_000L)
      }
    }
  }

  private fun recalculateDerivedState(state: PrayerUiState, nowMillis: Long): PrayerUiState {
    val prefs = state.preferences
    val (today, tomorrow) =
      KemenagPrayerCalculator.calculateTodayAndTomorrow(
        nowMillis = nowMillis,
        latitude = prefs.latitude,
        longitude = prefs.longitude,
        timeZoneId = prefs.timeZoneId,
        locationName = prefs.locationName,
      )

    val orderedEvents = buildList {
      val orderedTypes =
        listOf(
          PrayerType.IMSAK,
          PrayerType.SUBUH,
          PrayerType.TERBIT,
          PrayerType.DZUHUR,
          PrayerType.ASHAR,
          PrayerType.MAGHRIB,
          PrayerType.ISYA,
        )
      orderedTypes.forEach { add(today.slotOf(it) to false) }
      orderedTypes.forEach { add(tomorrow.slotOf(it) to true) }
    }

    val nextPair = orderedEvents.firstOrNull { (slot, _) -> slot.startEpochMillis > nowMillis }
    val nextEvent =
      nextPair?.let { (slot, isTomorrow) ->
        NextEventUi(
          type = slot.type,
          targetMillis = slot.startEpochMillis,
          remainingMillis = (slot.startEpochMillis - nowMillis).coerceAtLeast(0L),
          isTomorrow = isTomorrow,
        )
      }

    val activeSlot = findActiveObligatorySlot(nowMillis, today, prefs)
    val activePrayer =
      activeSlot?.let { slot ->
        val endMillis = slot.endEpochMillis ?: slot.startEpochMillis
        ActivePrayerUi(
          type = slot.type,
          startMillis = slot.startEpochMillis,
          endMillis = endMillis,
          remainingMillis = (endMillis - nowMillis).coerceAtLeast(0L),
        )
      }

    return state.copy(
      nowMillis = nowMillis,
      todaySchedule = today,
      tomorrowSchedule = tomorrow,
      nextEvent = nextEvent,
      activePrayer = activePrayer,
    )
  }

  private fun findActiveObligatorySlot(
    nowMillis: Long,
    today: DailyPrayerSchedule,
    prefs: PrayerPreferences,
  ): PrayerSlot? {
    // Check today's obligatory prayers first
    PrayerType.obligatoryPrayers.forEach { type ->
      val slot = today.slotOf(type)
      val end = slot.endEpochMillis ?: return@forEach
      if (nowMillis in slot.startEpochMillis until end) {
        return slot
      }
    }

    // If before today's Subuh, check yesterday's Isya (which ends at today's Subuh)
    val todaySubuhStart = today.slotOf(PrayerType.SUBUH).startEpochMillis
    if (nowMillis < todaySubuhStart) {
      val tz = TimeZone.getTimeZone(prefs.timeZoneId)
      val cal =
        Calendar.getInstance(tz).apply {
          timeInMillis = nowMillis
          add(Calendar.DAY_OF_MONTH, -1)
        }
      val yesterday =
        KemenagPrayerCalculator.calculateForDate(
          year = cal.get(Calendar.YEAR),
          month = cal.get(Calendar.MONTH) + 1,
          day = cal.get(Calendar.DAY_OF_MONTH),
          latitude = prefs.latitude,
          longitude = prefs.longitude,
          timeZoneId = prefs.timeZoneId,
          locationName = prefs.locationName,
        )
      val yesterdayIsya = yesterday.slotOf(PrayerType.ISYA)
      val end = yesterdayIsya.endEpochMillis ?: todaySubuhStart
      if (nowMillis in yesterdayIsya.startEpochMillis until end) {
        return yesterdayIsya
      }
    }

    return null
  }

  fun refreshExactAlarmStatus() {
    _uiState.update {
      it.copy(canScheduleExactAlarms = PrayerAlarmScheduler.canScheduleExactAlarms(appContext))
    }
  }

  fun refreshGpsLocation() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingLocation = true) }
      val result = withContext(Dispatchers.IO) { LocationHelper.fetchCurrentLocation(appContext) }
      if (result != null) {
        repository.updateLocation(
          latitude = result.latitude,
          longitude = result.longitude,
          timeZoneId = result.timeZoneId,
          locationName = result.displayLabel,
          isGps = true,
        )
        _uiState.update {
          it.copy(
            isLoadingLocation = false,
            snackbarMessage = "Lokasi GPS diperbarui: ${result.displayLabel}",
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isLoadingLocation = false,
            snackbarMessage =
              "Sinyal GPS belum tersedia. Gunakan lokasi tersimpan atau Pilih Kota secara offline.",
          )
        }
      }
    }
  }

  fun selectCity(city: IndonesianCity) {
    viewModelScope.launch {
      repository.selectCity(city)
      _uiState.update {
        it.copy(snackbarMessage = "Jadwal diperbarui untuk ${city.name}, ${city.province}.")
      }
    }
  }

  fun toggleAlreadyPrayed(prayerType: PrayerType, checked: Boolean) {
    val today = _uiState.value.todaySchedule ?: return
    viewModelScope.launch {
      repository.setAlreadyPrayed(prayerType, checked, today.dateKey)
      if (checked) {
        PrayerAlarmScheduler.cancelRemindersForPrayer(
          context = appContext,
          year = today.year,
          month = today.month,
          day = today.day,
          prayerType = prayerType,
        )
        _uiState.update {
          it.copy(
            snackbarMessage =
              "Sudah shalat ${prayerType.displayName}. Pengingat H-30 & H-15 hari ini dibatalkan."
          )
        }
      } else {
        _uiState.update {
          it.copy(snackbarMessage = "Pengingat ${prayerType.displayName} diaktifkan kembali.")
        }
      }
    }
  }

  fun setQuickOverride(prayerType: PrayerType, override: QuickSoundOverride) {
    viewModelScope.launch {
      repository.setQuickOverride(prayerType, override)
      _uiState.update {
        it.copy(snackbarMessage = "Mode suara ${prayerType.displayName}: ${override.label}")
      }
    }
  }

  fun syncDraftFromSaved() {
    val prefs = _uiState.value.preferences
    _uiState.update {
      it.copy(
        draftPrayerEntry = prefs.prayerEntrySound,
        draftImsakEntry = prefs.imsakEntrySound,
        draftExpiryReminder = prefs.expiryReminderSound,
      )
    }
  }

  fun updateDraftMode(category: SoundCategory, mode: SoundMode) {
    _uiState.update { state ->
      when (category) {
        SoundCategory.PRAYER_ENTRY ->
          state.copy(draftPrayerEntry = state.draftPrayerEntry.copy(mode = mode))
        SoundCategory.IMSAK_ENTRY ->
          state.copy(draftImsakEntry = state.draftImsakEntry.copy(mode = mode))
        SoundCategory.EXPIRY_REMINDER ->
          state.copy(draftExpiryReminder = state.draftExpiryReminder.copy(mode = mode))
      }
    }
  }

  fun onAudioFilePicked(category: SoundCategory, uri: Uri) {
    viewModelScope.launch {
      val copied =
        withContext(Dispatchers.IO) {
          CustomAudioStorage.copyPickedAudioToInternal(appContext, category, uri)
        }
      if (copied == null) {
        _uiState.update {
          it.copy(snackbarMessage = "Gagal menyalin file audio. Pastikan file dapat dibaca.")
        }
        return@launch
      }
      _uiState.update { state ->
        val updatedSetting =
          SoundCategorySetting(
            mode = SoundMode.CUSTOM_FILE,
            customFileName = copied.savedFileName,
            customDisplayName = copied.displayName,
          )
        when (category) {
          SoundCategory.PRAYER_ENTRY -> state.copy(draftPrayerEntry = updatedSetting)
          SoundCategory.IMSAK_ENTRY -> state.copy(draftImsakEntry = updatedSetting)
          SoundCategory.EXPIRY_REMINDER -> state.copy(draftExpiryReminder = updatedSetting)
        }
      }
      _uiState.update {
        it.copy(snackbarMessage = "File audio tersimpan: ${copied.displayName}")
      }
    }
  }

  fun previewDraftSound(category: SoundCategory) {
    val state = _uiState.value
    val setting =
      when (category) {
        SoundCategory.PRAYER_ENTRY -> state.draftPrayerEntry
        SoundCategory.IMSAK_ENTRY -> state.draftImsakEntry
        SoundCategory.EXPIRY_REMINDER -> state.draftExpiryReminder
      }
    audioPreviewPlayer.playPreview(category, setting) { errorMsg ->
      _uiState.update { it.copy(snackbarMessage = errorMsg) }
    }
  }

  fun stopPreviewSound() {
    audioPreviewPlayer.stop()
  }

  fun saveAndApplySoundSettings() {
    audioPreviewPlayer.stop()
    viewModelScope.launch {
      val state = _uiState.value
      val (oldVersion, newVersion) =
        repository.saveSoundSettingsAndBumpChannelVersion(
          prayerEntry = state.draftPrayerEntry,
          imsakEntry = state.draftImsakEntry,
          expiryReminder = state.draftExpiryReminder,
        )
      val updatedPrefs = repository.getCurrent()
      withContext(Dispatchers.Default) {
        NotificationChannelManager.rotateAndCreateChannels(
          context = appContext,
          preferences = updatedPrefs,
          oldVersion = oldVersion,
          newVersion = newVersion,
        )
        PrayerAlarmScheduler.rescheduleAll(appContext, updatedPrefs)
      }
      _uiState.update {
        it.copy(
          snackbarMessage =
            "Pengaturan disimpan (Channel v$newVersion) & seluruh alarm dijadwalkan ulang."
        )
      }
    }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    audioPreviewPlayer.stop()
  }
}
