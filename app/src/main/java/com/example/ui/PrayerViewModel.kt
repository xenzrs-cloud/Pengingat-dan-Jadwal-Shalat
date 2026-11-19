package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.NotificationChannelManager
import com.example.alarm.NotificationPreviewMoment
import com.example.alarm.NotificationThemeHelper
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmSoundService
import com.example.audio.AudioPreviewPlayer
import com.example.data.CustomAudioStorage
import com.example.data.LocationHelper
import com.example.data.PrayerPreferencesRepository
import com.example.domain.AppThemeMode
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
import com.example.domain.WidgetThemeMode
import com.example.widget.DynamicDayPhase
import com.example.widget.WidgetThemeHelper
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
  val isTomorrow: Boolean,
)

data class ActivePrayerUi(
  val type: PrayerType,
  val startMillis: Long,
  val endMillis: Long,
)

data class CountdownUiState(
  val nextRemainingMillis: Long = 0L,
  val activeRemainingMillis: Long = 0L,
  val activeProgress: Float = 0f,
)

data class PrayerUiState(
  val todaySchedule: DailyPrayerSchedule? = null,
  val tomorrowSchedule: DailyPrayerSchedule? = null,
  val formattedDate: String = "",
  val dynamicPhase: DynamicDayPhase = DynamicDayPhase.DAY,
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

private data class AlarmScheduleKey(
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val prayerEntry: SoundCategorySetting,
  val imsakEntry: SoundCategorySetting,
  val expiryReminder: SoundCategorySetting,
  val quickOverrides: Map<PrayerType, QuickSoundOverride>,
  val checkedDateKey: String,
  val checkedPrayers: Set<PrayerType>,
  val channelVersion: Int,
)

private data class ScheduleCacheKey(
  val dateKey: String,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val locationName: String,
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

  private val _countdownState = MutableStateFlow(CountdownUiState())
  val countdownState: StateFlow<CountdownUiState> = _countdownState.asStateFlow()

  private var lastComputedDateKey: String = ""
  private var lastAlarmKey: AlarmScheduleKey? = null
  private var lastScheduleKey: ScheduleCacheKey? = null
  private var cachedToday: DailyPrayerSchedule? = null
  private var cachedTomorrow: DailyPrayerSchedule? = null
  private var cachedDateText: String = ""
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

        val (today, tomorrow, dateText) =
          withContext(Dispatchers.Default) {
            ensureSchedulesCached(now, todayKey, prefs)
          }

        _uiState.update { state ->
          val updated =
            state.copy(
              preferences = prefs,
              todaySchedule = today,
              tomorrowSchedule = tomorrow,
              formattedDate = dateText,
              draftPrayerEntry =
                if (!draftInitialized) prefs.prayerEntrySound else state.draftPrayerEntry,
              draftImsakEntry =
                if (!draftInitialized) prefs.imsakEntrySound else state.draftImsakEntry,
              draftExpiryReminder =
                if (!draftInitialized) prefs.expiryReminderSound else state.draftExpiryReminder,
            )
          draftInitialized = true
          updateWindowAndPhase(updated, today, tomorrow, prefs, now)
        }
        updateCountdownValues(now)

        val alarmKey =
          AlarmScheduleKey(
            latitude = prefs.latitude,
            longitude = prefs.longitude,
            timeZoneId = prefs.timeZoneId,
            prayerEntry = prefs.prayerEntrySound,
            imsakEntry = prefs.imsakEntrySound,
            expiryReminder = prefs.expiryReminderSound,
            quickOverrides = prefs.quickOverrides,
            checkedDateKey = prefs.checkedDateKey,
            checkedPrayers = prefs.checkedPrayers,
            channelVersion = prefs.channelVersion,
          )
        if (alarmKey != lastAlarmKey) {
          lastAlarmKey = alarmKey
          withContext(Dispatchers.Default) {
            PrayerAlarmScheduler.rescheduleAll(appContext, prefs, now)
          }
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
        val prefs = currentState.preferences
        val tzId = prefs.timeZoneId
        val currentDateKey = KemenagPrayerCalculator.dateKeyForMillis(now, tzId)

        val dateChanged =
          lastComputedDateKey.isNotEmpty() && lastComputedDateKey != currentDateKey
        lastComputedDateKey = currentDateKey

        if (dateChanged) {
          repository.resetCheckedPrayersIfDateChanged(currentDateKey)
          val (today, tomorrow, dateText) =
            withContext(Dispatchers.Default) {
              ensureSchedulesCached(now, currentDateKey, prefs)
            }
          _uiState.update { state ->
            updateWindowAndPhase(
              state.copy(
                todaySchedule = today,
                tomorrowSchedule = tomorrow,
                formattedDate = dateText,
              ),
              today,
              tomorrow,
              prefs,
              now,
            )
          }
          withContext(Dispatchers.Default) {
            PrayerAlarmScheduler.rescheduleAll(appContext, prefs, now)
          }
        } else {
          // Only update PrayerUiState when an active/next prayer boundary or phase changes
          val today = currentState.todaySchedule
          val tomorrow = currentState.tomorrowSchedule
          if (today != null && tomorrow != null) {
            val phaseNow = WidgetThemeHelper.resolveDynamicPhase(now, tzId)
            val nextBoundaryPassed =
              currentState.nextEvent?.let { now >= it.targetMillis } ?: true
            val activeBoundaryPassed =
              currentState.activePrayer?.let { now >= it.endMillis } ?: false
            if (phaseNow != currentState.dynamicPhase || nextBoundaryPassed || activeBoundaryPassed) {
              _uiState.update { state ->
                updateWindowAndPhase(state, today, tomorrow, prefs, now)
              }
            }
          }
        }

        updateCountdownValues(now)
        delay(1_000L)
      }
    }
  }

  private fun ensureSchedulesCached(
    nowMillis: Long,
    todayKey: String,
    prefs: PrayerPreferences,
  ): Triple<DailyPrayerSchedule, DailyPrayerSchedule, String> {
    val key =
      ScheduleCacheKey(
        dateKey = todayKey,
        latitude = prefs.latitude,
        longitude = prefs.longitude,
        timeZoneId = prefs.timeZoneId,
        locationName = prefs.locationName,
      )
    val existingToday = cachedToday
    val existingTomorrow = cachedTomorrow
    if (key == lastScheduleKey && existingToday != null && existingTomorrow != null) {
      return Triple(existingToday, existingTomorrow, cachedDateText)
    }

    val (today, tomorrow) =
      KemenagPrayerCalculator.calculateTodayAndTomorrow(
        nowMillis = nowMillis,
        latitude = prefs.latitude,
        longitude = prefs.longitude,
        timeZoneId = prefs.timeZoneId,
        locationName = prefs.locationName,
      )
    val dateText = KemenagPrayerCalculator.formatIndonesianDate(nowMillis, prefs.timeZoneId)
    lastScheduleKey = key
    cachedToday = today
    cachedTomorrow = tomorrow
    cachedDateText = dateText
    return Triple(today, tomorrow, dateText)
  }

  private fun updateWindowAndPhase(
    state: PrayerUiState,
    today: DailyPrayerSchedule,
    tomorrow: DailyPrayerSchedule,
    prefs: PrayerPreferences,
    nowMillis: Long,
  ): PrayerUiState {
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
    var nextEvent: NextEventUi? = null
    for (type in orderedTypes) {
      val slot = today.slotOf(type)
      if (slot.startEpochMillis > nowMillis) {
        nextEvent = NextEventUi(slot.type, slot.startEpochMillis, isTomorrow = false)
        break
      }
    }
    if (nextEvent == null) {
      for (type in orderedTypes) {
        val slot = tomorrow.slotOf(type)
        if (slot.startEpochMillis > nowMillis) {
          nextEvent = NextEventUi(slot.type, slot.startEpochMillis, isTomorrow = true)
          break
        }
      }
    }

    val activeSlot = findActiveObligatorySlot(nowMillis, today, prefs)
    val activePrayer =
      activeSlot?.let { slot ->
        ActivePrayerUi(
          type = slot.type,
          startMillis = slot.startEpochMillis,
          endMillis = slot.endEpochMillis ?: slot.startEpochMillis,
        )
      }

    val phase = WidgetThemeHelper.resolveDynamicPhase(nowMillis, prefs.timeZoneId)
    return state.copy(
      dynamicPhase = phase,
      nextEvent = nextEvent,
      activePrayer = activePrayer,
    )
  }

  private fun updateCountdownValues(nowMillis: Long) {
    val state = _uiState.value
    val nextRemaining =
      state.nextEvent?.let { (it.targetMillis - nowMillis).coerceAtLeast(0L) } ?: 0L
    val active = state.activePrayer
    val activeRemaining =
      active?.let { (it.endMillis - nowMillis).coerceAtLeast(0L) } ?: 0L
    val activeProgress =
      if (active != null && active.endMillis > active.startMillis) {
        ((nowMillis - active.startMillis).toFloat() / (active.endMillis - active.startMillis).toFloat())
          .coerceIn(0f, 1f)
      } else {
        0f
      }
    _countdownState.value =
      CountdownUiState(
        nextRemainingMillis = nextRemaining,
        activeRemainingMillis = activeRemaining,
        activeProgress = activeProgress,
      )
  }

  private fun findActiveObligatorySlot(
    nowMillis: Long,
    today: DailyPrayerSchedule,
    prefs: PrayerPreferences,
  ): PrayerSlot? {
    PrayerType.obligatoryPrayers.forEach { type ->
      val slot = today.slotOf(type)
      val end = slot.endEpochMillis ?: return@forEach
      if (nowMillis in slot.startEpochMillis until end) {
        return slot
      }
    }

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

  fun setAppThemeMode(mode: AppThemeMode) {
    val optimisticPrefs = _uiState.value.preferences.copy(appThemeMode = mode)
    _uiState.update {
      it.copy(
        preferences = optimisticPrefs,
        snackbarMessage = "Tema aplikasi diubah: ${mode.label}",
      )
    }
    viewModelScope.launch {
      repository.setAppThemeMode(mode)
    }
  }

  fun setWidgetThemeMode(mode: WidgetThemeMode) {
    val optimisticPrefs = _uiState.value.preferences.copy(widgetThemeMode = mode)
    _uiState.update {
      it.copy(
        preferences = optimisticPrefs,
        snackbarMessage = "Tema widget diubah: ${mode.label}",
      )
    }
    viewModelScope.launch {
      repository.setWidgetThemeMode(mode)
      withContext(Dispatchers.IO) {
        com.example.widget.PrayerScheduleWidgetProvider.updateAllWidgetsWithPrefs(
          appContext,
          optimisticPrefs,
        )
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

  fun sendLiveNotificationPreview(
    moment: NotificationPreviewMoment,
    soundSetting: SoundCategorySetting,
  ) {
    viewModelScope.launch {
      val state = _uiState.value
      val prefs = state.preferences
      val schedule = state.todaySchedule
      val sampleSlot =
        schedule?.slotOf(
          state.activePrayer?.type
            ?: state.nextEvent?.type?.takeIf { it.isObligatory }
            ?: PrayerType.DZUHUR
        )
      val prayerName = sampleSlot?.type?.displayName ?: "Dzuhur"
      val startClock =
        sampleSlot?.let { KemenagPrayerCalculator.formatClock(it.startEpochMillis, prefs.timeZoneId) }
          ?: "11:45"
      val endClock =
        sampleSlot?.endEpochMillis?.let {
          KemenagPrayerCalculator.formatClock(it, prefs.timeZoneId)
        } ?: "15:01"

      val category =
        when (moment) {
          NotificationPreviewMoment.ENTRY -> SoundCategory.PRAYER_ENTRY
          NotificationPreviewMoment.H30,
          NotificationPreviewMoment.H15 -> SoundCategory.EXPIRY_REMINDER
        }

      val title =
        when (moment) {
          NotificationPreviewMoment.ENTRY -> "$prayerName • $startClock"
          NotificationPreviewMoment.H30 -> "$prayerName • 30 mnt lagi"
          NotificationPreviewMoment.H15 -> "$prayerName • 15 mnt lagi!"
        }
      val body =
        when (moment) {
          NotificationPreviewMoment.ENTRY -> "Masuk waktu • Habis $endClock"
          NotificationPreviewMoment.H30 -> "Batas waktu pukul $endClock"
          NotificationPreviewMoment.H15 -> "Segera shalat • Habis $endClock"
        }

      val hasAudibleSound =
        soundSetting.mode == SoundMode.DEFAULT_ADZAN || soundSetting.mode == SoundMode.CUSTOM_FILE
      val loopAudio = moment != NotificationPreviewMoment.ENTRY
      val notificationId = 8800 + moment.ordinal

      val shown =
        withContext(Dispatchers.Default) {
          var startedService = false
          if (hasAudibleSound) {
            startedService =
              PrayerAlarmSoundService.startAlarm(
                context = appContext,
                notificationId = notificationId,
                title = title,
                body = body,
                isUrgent = moment == NotificationPreviewMoment.H15,
                appThemeMode = prefs.appThemeMode,
                timeZoneId = prefs.timeZoneId,
                prayerType = sampleSlot?.type ?: PrayerType.DZUHUR,
                isTestPreview = true,
                soundMode = soundSetting.mode,
                customFileName = soundSetting.customFileName,
                loopAudio = loopAudio,
              )
          }
          if (startedService) {
            true
          } else {
            val previewChannelId =
              NotificationChannelManager.ensurePreviewChannel(appContext, category, soundSetting)
            NotificationThemeHelper.buildAndShowNotification(
              context = appContext,
              notificationId = notificationId,
              channelId = previewChannelId,
              title = title,
              body = body,
              isUrgent = moment == NotificationPreviewMoment.H15,
              appThemeMode = prefs.appThemeMode,
              timeZoneId = prefs.timeZoneId,
              prayerType = sampleSlot?.type ?: PrayerType.DZUHUR,
              isTestPreview = true,
              isInsistent = hasAudibleSound && loopAudio,
            )
          }
        }

      _uiState.update {
        it.copy(
          snackbarMessage =
            if (shown) {
              when {
                hasAudibleSound && !loopAudio ->
                  "Uji notifikasi Masuk aktif — suara diputar 1x hingga azan/audio selesai."
                hasAudibleSound ->
                  "Uji notifikasi ${moment.label} aktif & berbunyi berulang hingga ditutup."
                else ->
                  "Uji notifikasi ${moment.label} dikirim (${soundSetting.mode.label})."
              }
            } else {
              "Izin notifikasi belum aktif. Aktifkan izin notifikasi terlebih dahulu."
            }
        )
      }
    }
  }

  fun showSnackbar(message: String) {
    _uiState.update { it.copy(snackbarMessage = message) }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    audioPreviewPlayer.stop()
  }
}
