package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DailyPrayerSchedule
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerType
import com.example.domain.WidgetThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.widget.DynamicDayPhase

enum class WidgetStyleOption {
  SIZE_2X2,
  SIZE_3X2,
  SIZE_4X2,
}

@Composable
fun WidgetOptionsDialog(
  uiState: PrayerUiState,
  onSelectThemeMode: (WidgetThemeMode) -> Unit,
  onPinWidgetStyle: (WidgetStyleOption) -> Unit,
  onOpenAppSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val selectedTheme = uiState.preferences.widgetThemeMode
  var previewStyle by remember { mutableStateOf(WidgetStyleOption.SIZE_2X2) }
  val schedule = uiState.todaySchedule

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Mosque,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Column {
          Text("Widget Layar Utama")
          Text(
            text = "Pilih ukuran 2x2, 3x2, atau 4x2",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(430.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        // Live Preview across 2x2, 3x2, 4x2
        if (schedule != null) {
          item {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                PreviewStyleTab(
                  label = "2x2",
                  selected = previewStyle == WidgetStyleOption.SIZE_2X2,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previewStyle = WidgetStyleOption.SIZE_2X2
                  },
                  modifier = Modifier.weight(1f),
                )
                PreviewStyleTab(
                  label = "3x2",
                  selected = previewStyle == WidgetStyleOption.SIZE_3X2,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previewStyle = WidgetStyleOption.SIZE_3X2
                  },
                  modifier = Modifier.weight(1f),
                )
                PreviewStyleTab(
                  label = "4x2",
                  selected = previewStyle == WidgetStyleOption.SIZE_4X2,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previewStyle = WidgetStyleOption.SIZE_4X2
                  },
                  modifier = Modifier.weight(1f),
                )
              }

              WidgetLivePreviewCard(
                styleOption = previewStyle,
                themeMode = selectedTheme,
                schedule = schedule,
                nextPrayerType = uiState.nextEvent?.type,
              )

              val selectedSizeText =
                when (previewStyle) {
                  WidgetStyleOption.SIZE_2X2 -> "2x2"
                  WidgetStyleOption.SIZE_3X2 -> "3x2"
                  WidgetStyleOption.SIZE_4X2 -> "4x2"
                }
              Button(
                onClick = { onPinWidgetStyle(previewStyle) },
                modifier = Modifier.fillMaxWidth().testTag("pin_selected_widget_button"),
                shape = RoundedCornerShape(12.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambahkan Widget $selectedSizeText ke Layar Utama")
              }
            }
          }
        }

        // Pilihan Tema Khusus Widget (tidak memengaruhi tema aplikasi)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Tema Khusus Widget",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Pilihan warna widget terpisah dan tidak memengaruhi tema aplikasi.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WidgetThemeMode.entries.forEach { mode ->
              val isSelected = mode == selectedTheme
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(
                      selected = isSelected,
                      onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectThemeMode(mode)
                      },
                      role = Role.RadioButton,
                    )
                    .testTag("widget_theme_${mode.id}"),
                shape = RoundedCornerShape(12.dp),
                color =
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border =
                  if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                  else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  RadioButton(selected = isSelected, onClick = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = mode.label,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = mode.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        // 3 Pilihan Ukuran Widget untuk Dipasang
        item {
          Text(
            text = "Pasang Widget (3 Pilihan Ukuran)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
          )
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetOptionPinCard(
              title = "Widget 2x2 — Jadwal Mini",
              subtitle = "6 waktu harian (Imsak hingga Isya) ringkas hemat ruang.",
              icon = Icons.Rounded.GridView,
              onPin = { onPinWidgetStyle(WidgetStyleOption.SIZE_2X2) },
              testTag = "pin_widget_2x2_button",
            )
            WidgetOptionPinCard(
              title = "Widget 3x2 — Berikutnya + Jadwal",
              subtitle = "Panel kiri fokus waktu berikutnya, panel kanan 6 jadwal harian.",
              icon = Icons.Rounded.ViewAgenda,
              onPin = { onPinWidgetStyle(WidgetStyleOption.SIZE_3X2) },
              testTag = "pin_widget_3x2_button",
            )
            WidgetOptionPinCard(
              title = "Widget 4x2 — Jadwal Lengkap",
              subtitle = "Banner lebar dengan status shalat aktif dan 6 waktu lega.",
              icon = Icons.Rounded.ViewWeek,
              onPin = { onPinWidgetStyle(WidgetStyleOption.SIZE_4X2) },
              testTag = "pin_widget_4x2_button",
            )
          }
        }

        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Widget tidak muncul setelah ditekan?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "Pada sebagian HP (Xiaomi/MIUI, Oppo, Vivo), aktifkan izin \"Pintasan layar utama\" di menu Info Aplikasi lalu tekan kembali tombol Tambahkan Widget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth().testTag("open_widget_permission_settings_button"),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buka Info Aplikasi & Izin")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Selesai") }
    },
  )
}

@Composable
private fun PreviewStyleTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Box(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WidgetLivePreviewCard(
  styleOption: WidgetStyleOption,
  themeMode: WidgetThemeMode,
  schedule: DailyPrayerSchedule,
  nextPrayerType: PrayerType?,
) {
  val phase =
    when (themeMode) {
      WidgetThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
      WidgetThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
      WidgetThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
      WidgetThemeMode.DEFAULT -> null
    }

  val bgRes =
    when (phase) {
      DynamicDayPhase.DAY -> R.drawable.bg_widget_mosque_day
      DynamicDayPhase.SUNSET -> R.drawable.bg_widget_mosque_sunset
      DynamicDayPhase.NIGHT -> R.drawable.bg_widget_mosque_night
      DynamicDayPhase.DAWN -> R.drawable.bg_widget_mosque_dawn
      null -> null
    }

  val isLightThemeContent = phase == DynamicDayPhase.DAY || phase == null
  val primaryText =
    when {
      phase == null -> MaterialTheme.colorScheme.onSurface
      isLightThemeContent -> Color(0xFF0F172A)
      else -> Color.White
    }
  val secondaryText =
    when {
      phase == null -> MaterialTheme.colorScheme.onSurfaceVariant
      isLightThemeContent -> Color(0xFF334155)
      else -> Color(0xFFE2E8F0)
    }
  val accentText =
    when {
      phase == null -> MaterialTheme.colorScheme.primary
      isLightThemeContent -> Color(0xFF0F766E)
      else -> Color(0xFFFDE047)
    }
  val slotBg =
    when {
      phase == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
      isLightThemeContent -> Color.White.copy(alpha = 0.78f)
      else -> Color(0xFF0F172A).copy(alpha = 0.42f)
    }

  val previewWidth =
    when (styleOption) {
      WidgetStyleOption.SIZE_2X2 -> 164.dp
      WidgetStyleOption.SIZE_3X2 -> 236.dp
      WidgetStyleOption.SIZE_4X2 -> 280.dp
    }
  val previewHeight =
    when (styleOption) {
      WidgetStyleOption.SIZE_2X2 -> 164.dp
      WidgetStyleOption.SIZE_3X2 -> 152.dp
      WidgetStyleOption.SIZE_4X2 -> 140.dp
    }

  val nextSlot =
    remember(schedule, nextPrayerType) {
      schedule.slotOf(nextPrayerType ?: PrayerType.IMSAK)
    }
  val nextClock =
    remember(nextSlot.startEpochMillis, schedule.timeZoneId) {
      KemenagPrayerCalculator.formatClock(nextSlot.startEpochMillis, schedule.timeZoneId)
    }

  Surface(
    modifier = Modifier.width(previewWidth).height(previewHeight).clip(RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shadowElevation = 2.dp,
  ) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
      if (bgRes != null) {
        Image(
          painter = painterResource(id = bgRes),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = schedule.locationName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (styleOption == WidgetStyleOption.SIZE_4X2) {
            Text(
              text = "${nextSlot.type.displayName} $nextClock",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = accentText,
            )
          }
        }

        when (styleOption) {
          WidgetStyleOption.SIZE_2X2 -> {
            val pairs =
              listOf(
                PrayerType.IMSAK to PrayerType.SUBUH,
                PrayerType.DZUHUR to PrayerType.ASHAR,
                PrayerType.MAGHRIB to PrayerType.ISYA,
              )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              pairs.forEach { (left, right) ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  listOf(left, right).forEach { type ->
                    PreviewSlotPill(
                      label = type.displayName,
                      clock =
                        KemenagPrayerCalculator.formatClock(
                          schedule.slotOf(type).startEpochMillis,
                          schedule.timeZoneId,
                        ),
                      slotBg = slotBg,
                      labelColor = secondaryText,
                      timeColor = primaryText,
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }
          WidgetStyleOption.SIZE_3X2 -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                modifier = Modifier.weight(0.45f),
                shape = RoundedCornerShape(10.dp),
                color = slotBg,
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text(text = "Berikutnya", fontSize = 8.sp, color = secondaryText)
                  Text(
                    text = nextSlot.type.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                  )
                  Text(
                    text = nextClock,
                    fontSize = 16.sp,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentText,
                  )
                }
              }
              Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                listOf(
                    PrayerType.IMSAK to PrayerType.SUBUH,
                    PrayerType.DZUHUR to PrayerType.ASHAR,
                    PrayerType.MAGHRIB to PrayerType.ISYA,
                  )
                  .forEach { (l, r) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                      listOf(l, r).forEach { type ->
                        PreviewSlotPill(
                          label = type.displayName,
                          clock =
                            KemenagPrayerCalculator.formatClock(
                              schedule.slotOf(type).startEpochMillis,
                              schedule.timeZoneId,
                            ),
                          slotBg = slotBg,
                          labelColor = secondaryText,
                          timeColor = primaryText,
                          modifier = Modifier.weight(1f),
                        )
                      }
                    }
                  }
              }
            }
          }
          WidgetStyleOption.SIZE_4X2 -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(
                  listOf(PrayerType.IMSAK, PrayerType.SUBUH, PrayerType.DZUHUR),
                  listOf(PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
                )
                .forEach { rowTypes ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    rowTypes.forEach { type ->
                      PreviewSlotPill(
                        label = type.displayName,
                        clock =
                          KemenagPrayerCalculator.formatClock(
                            schedule.slotOf(type).startEpochMillis,
                            schedule.timeZoneId,
                          ),
                        slotBg = slotBg,
                        labelColor = secondaryText,
                        timeColor = primaryText,
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }
                }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PreviewSlotPill(
  label: String,
  clock: String,
  slotBg: Color,
  labelColor: Color,
  timeColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(modifier = modifier, shape = RoundedCornerShape(7.dp), color = slotBg) {
    Column(
      modifier = Modifier.padding(vertical = 2.dp, horizontal = 3.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = labelColor, maxLines = 1)
      Text(
        text = clock,
        fontSize = 10.sp,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        color = timeColor,
      )
    }
  }
}

@Composable
private fun WidgetOptionPinCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onPin: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Button(
        onClick = onPin,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Pasang ke Layar Utama")
      }
    }
  }
}
