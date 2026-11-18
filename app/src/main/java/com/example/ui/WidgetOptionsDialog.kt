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
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.widget.WidgetThemeHelper

enum class WidgetStyleOption {
  GRID_2X2,
  COMPACT_2X2,
}

@Composable
fun WidgetOptionsDialog(
  uiState: PrayerUiState,
  onSelectThemeMode: (WidgetThemeMode) -> Unit,
  onPinWidgetStyle: (WidgetStyleOption) -> Unit,
  onDismiss: () -> Unit,
) {
  val selectedTheme = uiState.preferences.widgetThemeMode
  var previewStyle by remember { mutableStateOf(WidgetStyleOption.GRID_2X2) }
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
        Text("Widget 2x2 & Tema")
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(430.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        // Live 2x2 Mini Preview
        if (schedule != null) {
          item {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                PreviewStyleTab(
                  label = "Jadwal Mini 2x2",
                  selected = previewStyle == WidgetStyleOption.GRID_2X2,
                  onClick = { previewStyle = WidgetStyleOption.GRID_2X2 },
                  modifier = Modifier.weight(1f),
                )
                PreviewStyleTab(
                  label = "Berikutnya 2x2",
                  selected = previewStyle == WidgetStyleOption.COMPACT_2X2,
                  onClick = { previewStyle = WidgetStyleOption.COMPACT_2X2 },
                  modifier = Modifier.weight(1f),
                )
              }

              WidgetLivePreviewCard(
                styleOption = previewStyle,
                themeMode = selectedTheme,
                schedule = schedule,
                nowMillis = uiState.nowMillis,
              )
            }
          }
        }

        // Pilihan Tema Background Widget
        item {
          Text(
            text = "Pilih Tema Background Widget",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WidgetThemeMode.entries.forEach { mode ->
              val isSelected = mode == selectedTheme
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .selectable(
                      selected = isSelected,
                      onClick = { onSelectThemeMode(mode) },
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

        // Pilihan Bentuk Widget 2x2 untuk Dipasang
        item {
          Text(
            text = "Pasang Widget (Ukuran 2x2)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
          )
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetOptionPinCard(
              title = "Opsi 1: Jadwal Mini (2x2)",
              subtitle = "6 waktu lengkap (Imsak, Subuh, Dzuhur, Ashar, Maghrib, Isya) dalam kotak 2x2.",
              icon = Icons.Rounded.GridView,
              onPin = { onPinWidgetStyle(WidgetStyleOption.GRID_2X2) },
              testTag = "pin_widget_grid_button",
            )
            WidgetOptionPinCard(
              title = "Opsi 2: Shalat Berikutnya (2x2)",
              subtitle = "Fokus waktu shalat selanjutnya dengan angka besar + status shalat aktif.",
              icon = Icons.Rounded.Schedule,
              onPin = { onPinWidgetStyle(WidgetStyleOption.COMPACT_2X2) },
              testTag = "pin_widget_compact_button",
            )
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
    modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
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
        fontWeight = FontWeight.SemiBold,
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
  nowMillis: Long,
) {
  val phase =
    when (themeMode) {
      WidgetThemeMode.DYNAMIC_MOSQUE ->
        WidgetThemeHelper.resolveDynamicPhase(nowMillis, schedule.timeZoneId)
      WidgetThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
      WidgetThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
      WidgetThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
      WidgetThemeMode.DEFAULT -> null
    }

  val bgRes =
    when (phase) {
      DynamicDayPhase.DAWN -> R.drawable.bg_widget_mosque_dawn
      DynamicDayPhase.DAY -> R.drawable.bg_widget_mosque_day
      DynamicDayPhase.SUNSET -> R.drawable.bg_widget_mosque_sunset
      DynamicDayPhase.NIGHT -> R.drawable.bg_widget_mosque_night
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

  Surface(
    modifier = Modifier.size(168.dp).clip(RoundedCornerShape(20.dp)),
    shape = RoundedCornerShape(20.dp),
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
        Text(
          text = schedule.locationName,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = primaryText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (styleOption == WidgetStyleOption.GRID_2X2) {
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
                  val clock =
                    KemenagPrayerCalculator.formatClock(
                      schedule.slotOf(type).startEpochMillis,
                      schedule.timeZoneId,
                    )
                  Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = slotBg,
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Text(
                        text = type.displayName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText,
                      )
                      Text(
                        text = clock,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                      )
                    }
                  }
                }
              }
            }
          }
        } else {
          val nextSlot =
            listOf(
                PrayerType.IMSAK,
                PrayerType.SUBUH,
                PrayerType.DZUHUR,
                PrayerType.ASHAR,
                PrayerType.MAGHRIB,
                PrayerType.ISYA,
              )
              .map { schedule.slotOf(it) }
              .firstOrNull { it.startEpochMillis > nowMillis } ?: schedule.slotOf(PrayerType.IMSAK)
          val clock =
            KemenagPrayerCalculator.formatClock(nextSlot.startEpochMillis, schedule.timeZoneId)

          Column {
            Text(text = "Waktu Berikutnya", fontSize = 9.sp, color = secondaryText)
            Text(
              text = nextSlot.type.displayName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = primaryText,
            )
            Text(
              text = clock,
              fontSize = 24.sp,
              fontFamily = JetBrainsMonoFamily,
              fontWeight = FontWeight.Bold,
              color = accentText,
            )
          }

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = slotBg,
          ) {
            Text(
              text =
                "Subuh ${
                  KemenagPrayerCalculator.formatClock(
                    schedule.slotOf(PrayerType.SUBUH).startEpochMillis,
                    schedule.timeZoneId,
                  )
                }",
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold,
              color = primaryText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetOptionPinCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
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
          imageVector = Icons.Rounded.AddToHomeScreen,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Pasang ke Layar Utama")
      }
    }
  }
}
