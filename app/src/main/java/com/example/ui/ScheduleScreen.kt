package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DailyPrayerSchedule
import com.example.domain.IndonesianCities
import com.example.domain.IndonesianCity
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerSlot
import com.example.domain.PrayerType
import com.example.domain.QuickSoundOverride
import com.example.ui.theme.JetBrainsMonoFamily
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
  onToggleAlreadyPrayed: (PrayerType, Boolean) -> Unit,
  onSelectQuickOverride: (PrayerType, QuickSoundOverride) -> Unit,
  onSnackbarShown: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var showCityPicker by remember { mutableStateOf(false) }

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
                onRefreshGps = onRefreshGps,
                onOpenCityPicker = { showCityPicker = true },
                onOpenSoundSettings = onOpenSoundSettings,
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
                onSelectOverride = { ov -> onSelectQuickOverride(PrayerType.IMSAK, ov) },
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
                  onToggleAlreadyPrayed(PrayerType.SUBUH, checked)
                },
                onSelectOverride = { ov -> onSelectQuickOverride(PrayerType.SUBUH, ov) },
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
                  onToggleAlreadyPrayed(prayerType, checked)
                },
                onSelectOverride = { ov -> onSelectQuickOverride(prayerType, ov) },
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          item {
            DashboardHeroSection(
              uiState = uiState,
              schedule = schedule,
              onRefreshGps = onRefreshGps,
              onOpenCityPicker = { showCityPicker = true },
              onOpenSoundSettings = onOpenSoundSettings,
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
              onSelectOverride = { ov -> onSelectQuickOverride(PrayerType.IMSAK, ov) },
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
                onToggleAlreadyPrayed(PrayerType.SUBUH, checked)
              },
              onSelectOverride = { ov -> onSelectQuickOverride(PrayerType.SUBUH, ov) },
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
              onToggleAlreadyPrayed = { checked -> onToggleAlreadyPrayed(prayerType, checked) },
              onSelectOverride = { ov -> onSelectQuickOverride(prayerType, ov) },
              modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
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
}

@Composable
private fun DashboardHeroSection(
  uiState: PrayerUiState,
  schedule: DailyPrayerSchedule,
  onRefreshGps: () -> Unit,
  onOpenCityPicker: () -> Unit,
  onOpenSoundSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val containerHighlight = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f)

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
    modifier = modifier.clip(RoundedCornerShape(24.dp)),
    shape = RoundedCornerShape(24.dp),
    shadowElevation = 3.dp,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            brush =
              Brush.linearGradient(
                colors = listOf(primaryColor, tertiaryColor)
              )
          )
          .drawBehind {
            drawCircle(
              color = containerHighlight,
              radius = size.width * 0.36f,
              center = Offset(size.width * 0.92f, size.height * 0.14f),
            )
            drawCircle(
              color = containerHighlight,
              radius = size.width * 0.22f,
              center = Offset(size.width * 0.10f, size.height * 0.95f),
            )
          }
          .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = dateText,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
            )
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
          ) {
            IconButton(
              onClick = onOpenSoundSettings,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sound_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_sound_settings),
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }

        // Location Info Pill
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector =
                if (uiState.preferences.isGpsLocation) Icons.Rounded.GpsFixed
                else Icons.Rounded.LocationCity,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = uiState.preferences.locationName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Koordinat: $coordText • ${schedule.timeZoneId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
              )
            }
          }
        }

        // Action Buttons Row
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
            color = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(12.dp),
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
                  color = MaterialTheme.colorScheme.primary,
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.MyLocation,
                  contentDescription = stringResource(R.string.cd_gps_refresh),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.action_refresh_gps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.LocationCity,
                contentDescription = stringResource(R.string.cd_choose_city),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.action_choose_city),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
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
    val isUrgentRemaining = active != null && active.remainingMillis <= 30 * 60_000L
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors =
        CardDefaults.cardColors(
          containerColor =
            if (isUrgentRemaining) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface
        ),
      border =
        BorderStroke(
          1.dp,
          if (isUrgentRemaining) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.outlineVariant,
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
            tint =
              if (isUrgentRemaining) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.tertiary,
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
            color =
              if (isUrgentRemaining) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.tertiary,
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
    shape = RoundedCornerShape(18.dp),
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
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Bottom action bar: Checkbox "Sudah Shalat" + Quick Sound Mode Dropdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onToggleAlreadyPrayed(!alreadyPrayed) }
              .padding(end = 8.dp)
              .testTag("already_prayed_toggle_${slot.type.id}"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = alreadyPrayed,
            onCheckedChange = onToggleAlreadyPrayed,
          )
          Column {
            Text(
              text = stringResource(R.string.label_already_prayed),
              style = MaterialTheme.typography.labelLarge,
              color =
                if (alreadyPrayed) MaterialTheme.colorScheme.primary
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
