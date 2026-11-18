package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AppThemeMode
import com.example.domain.DailyPrayerSchedule
import com.example.domain.IndonesianCities
import com.example.domain.IndonesianCity
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerPreferences
import com.example.domain.PrayerSlot
import com.example.domain.PrayerType
import com.example.domain.QuickSoundOverride
import com.example.domain.WidgetThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SoftInkOnAccent
import com.example.widget.DynamicDayPhase
import com.example.widget.WidgetThemeHelper
import java.util.Locale

@Composable
fun ScheduleScreen(
  uiState: PrayerUiState,
  hasNotificationPermission: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onRequestExactAlarmSettings: () -> Unit,
  onRefreshGps: () -> Unit,
  onSelectCity: (IndonesianCity) -> Unit,
  onOpenSoundSettings: () -> Unit,
  onSelectAppThemeMode: (AppThemeMode) -> Unit,
  onSelectWidgetThemeMode: (WidgetThemeMode) -> Unit,
  onPinWidgetStyle: (WidgetStyleOption) -> Unit,
  onToggleAlreadyPrayed: (PrayerType, Boolean) -> Unit,
  onSelectQuickOverride: (PrayerType, QuickSoundOverride) -> Unit,
  onSnackbarShown: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val haptics = LocalHapticFeedback.current
  val togglePrayedWithHaptics: (PrayerType, Boolean) -> Unit = { type, checked ->
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    onToggleAlreadyPrayed(type, checked)
  }
  val selectOverrideWithHaptics: (PrayerType, QuickSoundOverride) -> Unit = { type, ov ->
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    onSelectQuickOverride(type, ov)
  }
  var showCityPicker by remember { mutableStateOf(false) }
  var showWidgetOptions by remember { mutableStateOf(false) }
  var showThemePicker by remember { mutableStateOf(false) }

  val currentPhase =
    WidgetThemeHelper.resolveDynamicPhase(uiState.nowMillis, uiState.preferences.timeZoneId)
  val activeMosqueDrawable =
    resolveAppMosqueDrawable(uiState.preferences.appThemeMode, currentPhase)

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      onSnackbarShown()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    val schedule = uiState.todaySchedule
    if (schedule == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
      return@Scaffold
    }

    Box(modifier = Modifier.fillMaxSize()) {
      if (activeMosqueDrawable != null) {
        Image(
          painter = painterResource(id = activeMosqueDrawable),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          alpha = 0.22f,
          modifier = Modifier.fillMaxSize(),
        )
      }

      BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        val isWide = maxWidth >= 720.dp
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            LazyColumn(
              modifier = Modifier.weight(0.46f),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              contentPadding = PaddingValues(bottom = 24.dp),
            ) {
              item {
                DashboardHeroSection(
                  uiState = uiState,
                  schedule = schedule,
                  activePhase = currentPhase,
                  mosqueBgRes = activeMosqueDrawable,
                  onRefreshGps = onRefreshGps,
                  onOpenCityPicker = { showCityPicker = true },
                  onOpenThemePicker = { showThemePicker = true },
                  onOpenSoundSettings = onOpenSoundSettings,
                  onPinWidget = { showWidgetOptions = true },
                )
              }
            if (!hasNotificationPermission || !uiState.canScheduleExactAlarms) {
              item {
                PermissionAlertBanner(
                  hasNotificationPermission = hasNotificationPermission,
                  canScheduleExactAlarms = uiState.canScheduleExactAlarms,
                  onRequestNotificationPermission = onRequestNotificationPermission,
                  onRequestExactAlarmSettings = onRequestExactAlarmSettings,
                )
              }
            }
            item {
              DualCountdownSection(uiState = uiState, timeZoneId = schedule.timeZoneId)
            }
            item {
              DailyPrayerProgressCard(
                dateKey = schedule.dateKey,
                preferences = uiState.preferences,
                activePrayerType = uiState.activePrayer?.type,
                onTogglePrayer = togglePrayedWithHaptics,
              )
            }
          }

          LazyColumn(
            modifier = Modifier.weight(0.54f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            item {
              ImsakScheduleCard(
                slot = schedule.slotOf(PrayerType.IMSAK),
                subuhSlot = schedule.slotOf(PrayerType.SUBUH),
                override = uiState.preferences.overrideFor(PrayerType.IMSAK),
                isNext = uiState.nextEvent?.type == PrayerType.IMSAK,
                onSelectOverride = { ov -> selectOverrideWithHaptics(PrayerType.IMSAK, ov) },
              )
            }
            item {
              ObligatoryPrayerCard(
                slot = schedule.slotOf(PrayerType.SUBUH),
                isActive = uiState.activePrayer?.type == PrayerType.SUBUH,
                isNext = uiState.nextEvent?.type == PrayerType.SUBUH,
                alreadyPrayed =
                  uiState.preferences.isCheckedToday(PrayerType.SUBUH, schedule.dateKey),
                override = uiState.preferences.overrideFor(PrayerType.SUBUH),
                onToggleAlreadyPrayed = { checked ->
                  togglePrayedWithHaptics(PrayerType.SUBUH, checked)
                },
                onSelectOverride = { ov -> selectOverrideWithHaptics(PrayerType.SUBUH, ov) },
              )
            }
            item { SyuruqInfoStrip(slot = schedule.slotOf(PrayerType.TERBIT)) }
            items(
              items = listOf(PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
              key = { it.id },
            ) { prayerType ->
              ObligatoryPrayerCard(
                slot = schedule.slotOf(prayerType),
                isActive = uiState.activePrayer?.type == prayerType,
                isNext = uiState.nextEvent?.type == prayerType,
                alreadyPrayed =
                  uiState.preferences.isCheckedToday(prayerType, schedule.dateKey),
                override = uiState.preferences.overrideFor(prayerType),
                onToggleAlreadyPrayed = { checked ->
                  togglePrayedWithHaptics(prayerType, checked)
                },
                onSelectOverride = { ov -> selectOverrideWithHaptics(prayerType, ov) },
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          item {
            DashboardHeroSection(
              uiState = uiState,
              schedule = schedule,
              activePhase = currentPhase,
              mosqueBgRes = activeMosqueDrawable,
              onRefreshGps = onRefreshGps,
              onOpenCityPicker = { showCityPicker = true },
              onOpenThemePicker = { showThemePicker = true },
              onOpenSoundSettings = onOpenSoundSettings,
              onPinWidget = { showWidgetOptions = true },
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          if (!hasNotificationPermission || !uiState.canScheduleExactAlarms) {
            item {
              PermissionAlertBanner(
                hasNotificationPermission = hasNotificationPermission,
                canScheduleExactAlarms = uiState.canScheduleExactAlarms,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarmSettings = onRequestExactAlarmSettings,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
              )
            }
          }

          item {
            DualCountdownSection(
              uiState = uiState,
              timeZoneId = schedule.timeZoneId,
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          item {
            DailyPrayerProgressCard(
              dateKey = schedule.dateKey,
              preferences = uiState.preferences,
              activePrayerType = uiState.activePrayer?.type,
              onTogglePrayer = togglePrayedWithHaptics,
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          item {
            SectionHeaderRow(
              title = "Jadwal Hari Ini",
              subtitle = stringResource(R.string.label_kemenag_standard),
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          // Kartu Imsak paling atas
          item {
            ImsakScheduleCard(
              slot = schedule.slotOf(PrayerType.IMSAK),
              subuhSlot = schedule.slotOf(PrayerType.SUBUH),
              override = uiState.preferences.overrideFor(PrayerType.IMSAK),
              isNext = uiState.nextEvent?.type == PrayerType.IMSAK,
              onSelectOverride = { ov -> selectOverrideWithHaptics(PrayerType.IMSAK, ov) },
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          // Kartu Subuh
          item {
            ObligatoryPrayerCard(
              slot = schedule.slotOf(PrayerType.SUBUH),
              isActive = uiState.activePrayer?.type == PrayerType.SUBUH,
              isNext = uiState.nextEvent?.type == PrayerType.SUBUH,
              alreadyPrayed =
                uiState.preferences.isCheckedToday(PrayerType.SUBUH, schedule.dateKey),
              override = uiState.preferences.overrideFor(PrayerType.SUBUH),
              onToggleAlreadyPrayed = { checked ->
                togglePrayedWithHaptics(PrayerType.SUBUH, checked)
              },
              onSelectOverride = { ov -> selectOverrideWithHaptics(PrayerType.SUBUH, ov) },
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          // Strip informasi Terbit (Syuruq) sebagai batas akhir Subuh
          item {
            SyuruqInfoStrip(
              slot = schedule.slotOf(PrayerType.TERBIT),
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }

          // 4 Shalat Wajib lainnya
          items(
            items = listOf(PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
            key = { it.id },
          ) { prayerType ->
            ObligatoryPrayerCard(
              slot = schedule.slotOf(prayerType),
              isActive = uiState.activePrayer?.type == prayerType,
              isNext = uiState.nextEvent?.type == prayerType,
              alreadyPrayed = uiState.preferences.isCheckedToday(prayerType, schedule.dateKey),
              override = uiState.preferences.overrideFor(prayerType),
              onToggleAlreadyPrayed = { checked ->
                togglePrayedWithHaptics(prayerType, checked)
              },
              onSelectOverride = { ov -> selectOverrideWithHaptics(prayerType, ov) },
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

  if (showCityPicker) {
    IndonesianCityPickerDialog(
      onSelectCity = { city ->
        showCityPicker = false
        onSelectCity(city)
      },
      onDismiss = { showCityPicker = false },
    )
  }

  if (showWidgetOptions) {
    WidgetOptionsDialog(
      uiState = uiState,
      onSelectThemeMode = onSelectWidgetThemeMode,
      onPinWidgetStyle = { styleOption ->
        showWidgetOptions = false
        onPinWidgetStyle(styleOption)
      },
      onDismiss = { showWidgetOptions = false },
    )
  }

  if (showThemePicker) {
    ThemePickerDialog(
      selectedMode = uiState.preferences.appThemeMode,
      currentPhase = currentPhase,
      onSelectMode = onSelectAppThemeMode,
      onDismiss = { showThemePicker = false },
    )
  }
}

internal fun resolveEffectiveAppPhase(
  mode: AppThemeMode,
  currentPhase: DynamicDayPhase,
): DynamicDayPhase? =
  when (mode) {
    AppThemeMode.DEFAULT -> null
    AppThemeMode.DYNAMIC_MOSQUE -> currentPhase
    AppThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
    AppThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
    AppThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
  }

internal fun resolveAppMosqueDrawable(
  mode: AppThemeMode,
  currentPhase: DynamicDayPhase,
): Int? {
  val phase = resolveEffectiveAppPhase(mode, currentPhase) ?: return null
  return when (phase) {
    DynamicDayPhase.DAWN -> R.drawable.bg_widget_mosque_dawn
    DynamicDayPhase.DAY -> R.drawable.bg_widget_mosque_day
    DynamicDayPhase.SUNSET -> R.drawable.bg_widget_mosque_sunset
    DynamicDayPhase.NIGHT -> R.drawable.bg_widget_mosque_night
  }
}

private fun phaseAtmosphereLabel(
  mode: AppThemeMode,
  currentPhase: DynamicDayPhase,
): String? {
  val phase = resolveEffectiveAppPhase(mode, currentPhase) ?: return null
  val prefix = if (mode == AppThemeMode.DYNAMIC_MOSQUE) "Dinamis • " else ""
  return when (phase) {
    DynamicDayPhase.DAWN -> "${prefix}Suasana Fajar"
    DynamicDayPhase.DAY -> "${prefix}Suasana Siang"
    DynamicDayPhase.SUNSET -> "${prefix}Suasana Senja"
    DynamicDayPhase.NIGHT -> "${prefix}Suasana Malam"
  }
}

@Composable
private fun DashboardHeroSection(
  uiState: PrayerUiState,
  schedule: DailyPrayerSchedule,
  activePhase: DynamicDayPhase,
  mosqueBgRes: Int?,
  onRefreshGps: () -> Unit,
  onOpenCityPicker: () -> Unit,
  onOpenThemePicker: () -> Unit,
  onOpenSoundSettings: () -> Unit,
  onPinWidget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val isMosqueTheme = mosqueBgRes != null
  val effectiveAppPhase = resolveEffectiveAppPhase(uiState.preferences.appThemeMode, activePhase)
  val isDaySky = isMosqueTheme && effectiveAppPhase == DynamicDayPhase.DAY

  val heroTextColor = SoftInkOnAccent
  val containerHighlight = SoftInkOnAccent.copy(alpha = 0.08f)
  val defaultHeroGradient =
    if (isDark) listOf(Color(0xFF0E4F49), Color(0xFF0B3B4E))
    else listOf(Color(0xFF0F766E), Color(0xFF0E7490))

  // Translucent glass button tokens matched to the hero backdrop (never stark white)
  val primaryGlassFill =
    if (isDaySky) Color(0xFF083352).copy(alpha = 0.44f)
    else SoftInkOnAccent.copy(alpha = 0.22f)
  val primaryGlassBorder = SoftInkOnAccent.copy(alpha = 0.52f)
  val secondaryGlassFill =
    if (isDaySky) Color(0xFF083352).copy(alpha = 0.28f)
    else SoftInkOnAccent.copy(alpha = 0.13f)
  val secondaryGlassBorder = SoftInkOnAccent.copy(alpha = 0.34f)

  val atmosphereLabel =
    phaseAtmosphereLabel(uiState.preferences.appThemeMode, activePhase)

  val dateText =
    remember(schedule.dateKey, schedule.timeZoneId) {
      KemenagPrayerCalculator.formatIndonesianDate(uiState.nowMillis, schedule.timeZoneId)
    }
  val coordText =
    String.format(
      Locale.US,
      "%.4f, %.4f",
      uiState.preferences.latitude,
      uiState.preferences.longitude,
    )

  Surface(
    modifier = modifier.clip(RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    shadowElevation = 2.dp,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .then(
            if (isMosqueTheme) {
              Modifier.background(Color(0xFF0F172A))
            } else {
              Modifier.background(brush = Brush.linearGradient(colors = defaultHeroGradient))
                .drawBehind {
                  drawCircle(
                    color = containerHighlight,
                    radius = size.width * 0.34f,
                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                  )
                  drawCircle(
                    color = containerHighlight,
                    radius = size.width * 0.20f,
                    center = Offset(size.width * 0.10f, size.height * 0.95f),
                  )
                }
            }
          )
    ) {
      if (mosqueBgRes != null) {
        Image(
          painter = painterResource(id = mosqueBgRes),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize(),
        )
        val topScrim = if (isDaySky) Color(0xFF0C4A6E) else Color(0xFF020617)
        Box(
          modifier =
            Modifier.matchParentSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(topScrim.copy(alpha = 0.56f), topScrim.copy(alpha = 0.28f))
                )
              )
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              color = heroTextColor,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = dateText,
              style = MaterialTheme.typography.bodyMedium,
              color = heroTextColor.copy(alpha = 0.90f),
            )
            if (atmosphereLabel != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(100.dp),
                color = secondaryGlassFill,
                border = BorderStroke(1.dp, secondaryGlassBorder),
              ) {
                Text(
                  text = atmosphereLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = heroTextColor,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = CircleShape,
              color = secondaryGlassFill,
              border = BorderStroke(1.dp, secondaryGlassBorder),
            ) {
              IconButton(
                onClick = onOpenThemePicker,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("theme_picker_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Palette,
                  contentDescription = stringResource(R.string.cd_theme_picker),
                  tint = heroTextColor,
                )
              }
            }

            Surface(
              shape = CircleShape,
              color = secondaryGlassFill,
              border = BorderStroke(1.dp, secondaryGlassBorder),
            ) {
              IconButton(
                onClick = onPinWidget,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("pin_widget_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Widgets,
                  contentDescription = stringResource(R.string.cd_pin_widget),
                  tint = heroTextColor,
                )
              }
            }

            Surface(
              shape = CircleShape,
              color = secondaryGlassFill,
              border = BorderStroke(1.dp, secondaryGlassBorder),
            ) {
              IconButton(
                onClick = onOpenSoundSettings,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("sound_settings_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = stringResource(R.string.cd_sound_settings),
                  tint = heroTextColor,
                )
              }
            }
          }
        }

        // Location Info Pill (soft translucent glass)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = secondaryGlassFill,
          border = BorderStroke(1.dp, secondaryGlassBorder.copy(alpha = 0.6f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector =
                if (uiState.preferences.isGpsLocation) Icons.Rounded.GpsFixed
                else Icons.Rounded.LocationCity,
              contentDescription = null,
              tint = heroTextColor,
              modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = uiState.preferences.locationName,
                style = MaterialTheme.typography.titleSmall,
                color = heroTextColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Koordinat: $coordText • ${schedule.timeZoneId}",
                style = MaterialTheme.typography.labelSmall,
                color = heroTextColor.copy(alpha = 0.85f),
              )
            }
          }
        }

        // Action Buttons Row (Both translucent glass matching hero backdrop)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            modifier =
              Modifier.weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !uiState.isLoadingLocation, onClick = onRefreshGps)
                .testTag("refresh_gps_button"),
            color = primaryGlassFill,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, primaryGlassBorder),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              if (uiState.isLoadingLocation) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = heroTextColor,
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.MyLocation,
                  contentDescription = stringResource(R.string.cd_gps_refresh),
                  tint = heroTextColor,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.action_refresh_gps),
                style = MaterialTheme.typography.labelLarge,
                color = heroTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Surface(
            modifier =
              Modifier.weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenCityPicker)
                .testTag("choose_city_button"),
            color = secondaryGlassFill,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, secondaryGlassBorder),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.LocationCity,
                contentDescription = stringResource(R.string.cd_choose_city),
                tint = heroTextColor,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.action_choose_city),
                style = MaterialTheme.typography.labelLarge,
                color = heroTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionAlertBanner(
  hasNotificationPermission: Boolean,
  canScheduleExactAlarms: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onRequestExactAlarmSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Izin Alarm & Notifikasi Diperlukan",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
        text =
          "Agar adzan dan pengingat H-30/H-15 berbunyi tepat waktu saat layar terkunci, aktifkan izin berikut:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasNotificationPermission) {
          FilledTonalButton(
            onClick = onRequestNotificationPermission,
            modifier = Modifier.testTag("grant_notification_permission_button"),
          ) {
            Text("Izinkan Notifikasi")
          }
        }
        if (!canScheduleExactAlarms) {
          OutlinedButton(
            onClick = onRequestExactAlarmSettings,
            modifier = Modifier.testTag("grant_exact_alarm_button"),
          ) {
            Text("Izinkan Exact Alarm")
          }
        }
      }
    }
  }
}

@Composable
private fun DualCountdownSection(
  uiState: PrayerUiState,
  timeZoneId: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1. Hitung mundur menuju waktu berikutnya (termasuk Imsak jika sebelum Imsak)
    val nextEvent = uiState.nextEvent
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = stringResource(R.string.label_next_prayer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (nextEvent != null) {
          val clock = KemenagPrayerCalculator.formatClock(nextEvent.targetMillis, timeZoneId)
          Text(
            text =
              "${nextEvent.type.displayName} • $clock" +
                if (nextEvent.isTomorrow) " (Besok)" else "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = KemenagPrayerCalculator.formatCountdown(nextEvent.remainingMillis),
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontFeatureSettings = "tnum",
              ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        } else {
          Text(
            text = "--:--:--",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily),
          )
        }
      }
    }

    // 2. Hitung mundur sisa waktu menuju batas habis shalat yang sedang aktif
    val active = uiState.activePrayer
    val isUrgent15 = active != null && active.remainingMillis <= 15 * 60_000L
    val isWarning30 = active != null && active.remainingMillis <= 30 * 60_000L
    val activeWindowProgress =
      if (active != null && active.endMillis > active.startMillis) {
        ((uiState.nowMillis - active.startMillis).toFloat() /
            (active.endMillis - active.startMillis).toFloat())
          .coerceIn(0f, 1f)
      } else {
        0f
      }
    val animatedActiveProgress by
      animateFloatAsState(
        targetValue = activeWindowProgress,
        animationSpec = tween(durationMillis = 450),
        label = "activeWindowProgress",
      )
    val accentActiveColor =
      when {
        isUrgent15 -> MaterialTheme.colorScheme.error
        isWarning30 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
      }

    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors =
        CardDefaults.cardColors(
          containerColor =
            when {
              isUrgent15 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
              isWarning30 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
              else -> MaterialTheme.colorScheme.surface
            }
        ),
      border =
        BorderStroke(
          1.dp,
          if (isWarning30) accentActiveColor else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            tint = accentActiveColor,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = stringResource(R.string.label_active_remaining),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (active != null) {
          val endClock = KemenagPrayerCalculator.formatClock(active.endMillis, timeZoneId)
          Text(
            text = "${active.type.displayName} • Habis $endClock",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentActiveColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = KemenagPrayerCalculator.formatCountdown(active.remainingMillis),
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontFeatureSettings = "tnum",
              ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(2.dp))
          LinearProgressIndicator(
            progress = { animatedActiveProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)),
            color = accentActiveColor,
            trackColor = accentActiveColor.copy(alpha = 0.16f),
            strokeCap = StrokeCap.Round,
          )
        } else {
          Text(
            text = "Di luar waktu wajib",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Syuruq — Dzuhur",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun DailyPrayerProgressCard(
  dateKey: String,
  preferences: PrayerPreferences,
  activePrayerType: PrayerType?,
  onTogglePrayer: (PrayerType, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val obligatory = PrayerType.obligatoryPrayers
  val completedCount = obligatory.count { preferences.isCheckedToday(it, dateKey) }
  val progress by
    animateFloatAsState(
      targetValue = completedCount / 5f,
      animationSpec = tween(durationMillis = 400),
      label = "dailyPrayerProgress",
    )

  Card(
    modifier = modifier.testTag("daily_prayer_progress_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.TaskAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Progres Shalat Wajib",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          shape = RoundedCornerShape(100.dp),
          color =
            if (completedCount == 5) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "$completedCount / 5 Selesai",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color =
              if (completedCount == 5) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(100.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        obligatory.forEach { prayerType ->
          val checked = preferences.isCheckedToday(prayerType, dateKey)
          val isActive = activePrayerType == prayerType
          Surface(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onTogglePrayer(prayerType, !checked) }
                .semantics { role = Role.Checkbox }
                .testTag("progress_chip_${prayerType.id}"),
            shape = RoundedCornerShape(10.dp),
            color =
              when {
                checked -> MaterialTheme.colorScheme.primaryContainer
                isActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
              },
            border =
              BorderStroke(
                width = if (isActive && !checked) 1.5.dp else 1.dp,
                color =
                  when {
                    checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    isActive -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outlineVariant
                  },
              ),
          ) {
            Column(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(
                imageVector =
                  if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (checked) "${prayerType.displayName} selesai" else prayerType.displayName,
                tint =
                  when {
                    checked -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = prayerType.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (checked || isActive) FontWeight.Bold else FontWeight.Medium,
                color =
                  when {
                    checked -> MaterialTheme.colorScheme.onPrimaryContainer
                    isActive -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeaderRow(title: String, subtitle: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.padding(top = 4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ImsakScheduleCard(
  slot: PrayerSlot,
  subuhSlot: PrayerSlot,
  override: QuickSoundOverride,
  isNext: Boolean,
  onSelectOverride: (QuickSoundOverride) -> Unit,
  modifier: Modifier = Modifier,
) {
  val imsakClock = KemenagPrayerCalculator.formatClock(slot.startEpochMillis, slot.timeZoneId)
  val subuhClock = KemenagPrayerCalculator.formatClock(subuhSlot.startEpochMillis, slot.timeZoneId)

  Card(
    modifier = modifier.testTag("card_imsak"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
      ),
    border =
      BorderStroke(
        width = if (isNext) 2.dp else 1.dp,
        color = MaterialTheme.colorScheme.secondary,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Bedtime,
              contentDescription = stringResource(R.string.cd_imsak_icon),
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Imsak",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (isNext) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondary,
              ) {
                Text(
                  text = "Berikutnya",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = "${stringResource(R.string.label_imsak_subtitle)} ($subuhClock)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
          )
        }
      }

      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = imsakClock,
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontFamily = JetBrainsMonoFamily,
              fontFeatureSettings = "tnum",
            ),
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        QuickSoundOverrideDropdown(
          prayerType = PrayerType.IMSAK,
          currentOverride = override,
          onSelectOverride = onSelectOverride,
        )
      }
    }
  }
}

@Composable
private fun SyuruqInfoStrip(slot: PrayerSlot, modifier: Modifier = Modifier) {
  val terbitClock = KemenagPrayerCalculator.formatClock(slot.startEpochMillis, slot.timeZoneId)
  Surface(
    modifier = modifier.testTag("strip_syuruq"),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.WbTwilight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Terbit (Syuruq) • Batas akhir Subuh",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = terbitClock,
        style =
          MaterialTheme.typography.titleSmall.copy(
            fontFamily = JetBrainsMonoFamily,
            fontFeatureSettings = "tnum",
          ),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun ObligatoryPrayerCard(
  slot: PrayerSlot,
  isActive: Boolean,
  isNext: Boolean,
  alreadyPrayed: Boolean,
  override: QuickSoundOverride,
  onToggleAlreadyPrayed: (Boolean) -> Unit,
  onSelectOverride: (QuickSoundOverride) -> Unit,
  modifier: Modifier = Modifier,
) {
  val startClock = KemenagPrayerCalculator.formatClock(slot.startEpochMillis, slot.timeZoneId)
  val endClock =
    slot.endEpochMillis?.let { KemenagPrayerCalculator.formatClock(it, slot.timeZoneId) } ?: "--:--"

  val containerColor by
    animateColorAsState(
      targetValue =
        when {
          alreadyPrayed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
          else -> MaterialTheme.colorScheme.surface
        },
      label = "prayerCardContainer",
    )

  Card(
    modifier = modifier.testTag("card_prayer_${slot.type.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border =
      BorderStroke(
        width = if (isActive || isNext) 1.5.dp else 1.dp,
        color =
          when {
            isActive -> MaterialTheme.colorScheme.primary
            isNext -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outlineVariant
          },
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color =
              if (isActive) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = prayerIcon(slot.type),
                contentDescription = stringResource(R.string.cd_prayer_icon),
                tint =
                  if (isActive) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = slot.type.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (isActive) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primary,
                ) {
                  Text(
                    text = "Aktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              } else if (isNext) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                  Text(
                    text = "Berikutnya",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
            Text(
              text = "Masuk $startClock • Habis $endClock",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Text(
          text = startClock,
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontFamily = JetBrainsMonoFamily,
              fontFeatureSettings = "tnum",
            ),
          fontWeight = FontWeight.Bold,
          color =
            if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(modifier = Modifier.height(0.dp))

      // Bottom action bar: Tactile Pill "Sudah Shalat" + Quick Sound Mode Dropdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onToggleAlreadyPrayed(!alreadyPrayed) }
              .semantics { role = Role.Checkbox }
              .testTag("already_prayed_toggle_${slot.type.id}"),
          shape = RoundedCornerShape(12.dp),
          color =
            if (alreadyPrayed) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border =
            BorderStroke(
              1.dp,
              if (alreadyPrayed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
              else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector =
                if (alreadyPrayed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
              contentDescription = null,
              tint =
                if (alreadyPrayed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Column {
              Text(
                text =
                  if (alreadyPrayed) "Sudah Shalat"
                  else stringResource(R.string.label_already_prayed),
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (alreadyPrayed) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurface,
              )
              if (alreadyPrayed) {
                Text(
                  text = "Pengingat H-30/H-15 dibatalkan",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        QuickSoundOverrideDropdown(
          prayerType = slot.type,
          currentOverride = override,
          onSelectOverride = onSelectOverride,
        )
      }
    }
  }
}

@Composable
private fun QuickSoundOverrideDropdown(
  prayerType: PrayerType,
  currentOverride: QuickSoundOverride,
  onSelectOverride: (QuickSoundOverride) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  Box {
    Surface(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(10.dp))
          .clickable { expanded = true }
          .testTag("quick_override_button_${prayerType.id}"),
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector =
            when (currentOverride) {
              QuickSoundOverride.DEFAULT -> Icons.Rounded.VolumeUp
              QuickSoundOverride.VIBRATE -> Icons.Rounded.Vibration
              QuickSoundOverride.SILENT -> Icons.Rounded.VolumeOff
            },
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = currentOverride.label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
          imageVector = Icons.Rounded.ArrowDropDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      QuickSoundOverride.entries.forEach { option ->
        DropdownMenuItem(
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector =
                  when (option) {
                    QuickSoundOverride.DEFAULT -> Icons.Rounded.VolumeUp
                    QuickSoundOverride.VIBRATE -> Icons.Rounded.Vibration
                    QuickSoundOverride.SILENT -> Icons.Rounded.NotificationsOff
                  },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Text(option.label)
              if (option == currentOverride) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          },
          onClick = {
            expanded = false
            onSelectOverride(option)
          },
          modifier = Modifier.testTag("quick_override_option_${prayerType.id}_${option.id}"),
        )
      }
    }
  }
}

@Composable
private fun IndonesianCityPickerDialog(
  onSelectCity: (IndonesianCity) -> Unit,
  onDismiss: () -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredCities =
    remember(searchQuery) {
      if (searchQuery.isBlank()) IndonesianCities.all
      else {
        IndonesianCities.all.filter {
          it.name.contains(searchQuery, ignoreCase = true) ||
            it.province.contains(searchQuery, ignoreCase = true)
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Pilih Kota Indonesia") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari kota atau provinsi...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("city_search_input"),
        )
        LazyColumn(modifier = Modifier.height(300.dp)) {
          items(filteredCities, key = { it.name }) { city ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clickable { onSelectCity(city) }
                  .padding(vertical = 10.dp, horizontal = 8.dp)
                  .testTag("city_item_${city.name.lowercase().replace(" ", "_")}"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = city.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = city.province,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text =
                  when (city.timeZoneId) {
                    "Asia/Jakarta" -> "WIB"
                    "Asia/Makassar" -> "WITA"
                    else -> "WIT"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
  )
}

private fun prayerIcon(type: PrayerType): ImageVector =
  when (type) {
    PrayerType.IMSAK -> Icons.Rounded.Bedtime
    PrayerType.SUBUH -> Icons.Rounded.WbTwilight
    PrayerType.TERBIT -> Icons.Rounded.WbSunny
    PrayerType.DZUHUR -> Icons.Rounded.WbSunny
    PrayerType.ASHAR -> Icons.Rounded.Alarm
    PrayerType.MAGHRIB -> Icons.Rounded.WbTwilight
    PrayerType.ISYA -> Icons.Rounded.DarkMode
  }
