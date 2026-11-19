package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alarm.NotificationPreviewMoment
import com.example.alarm.NotificationThemeHelper
import com.example.domain.KemenagPrayerCalculator
import com.example.domain.PrayerType
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode
import com.example.ui.theme.SoftInkOnAccent

@Composable
fun NotificationPreviewCard(
  uiState: PrayerUiState,
  prayerEntrySetting: SoundCategorySetting,
  expiryReminderSetting: SoundCategorySetting,
  onSendLiveNotificationPreview: (NotificationPreviewMoment, SoundCategorySetting) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  var selectedMoment by remember { mutableStateOf(NotificationPreviewMoment.ENTRY) }

  val activeVisual =
    remember(uiState.preferences.appThemeMode, uiState.dynamicPhase, uiState.preferences.timeZoneId) {
      NotificationThemeHelper.resolveVisual(
        mode = uiState.preferences.appThemeMode,
        nowMillis = System.currentTimeMillis(),
        timeZoneId = uiState.preferences.timeZoneId,
      )
    }
  val accentColor by
    animateColorAsState(
      targetValue = Color(activeVisual.accentColor),
      animationSpec = tween(250),
      label = "notifAccent",
    )

  val schedule = uiState.todaySchedule
  val sampleSlot =
    remember(schedule, uiState.activePrayer, uiState.nextEvent) {
      schedule?.slotOf(
        uiState.activePrayer?.type
          ?: uiState.nextEvent?.type?.takeIf { it.isObligatory }
          ?: PrayerType.DZUHUR
      )
    }
  val prayerName = sampleSlot?.type?.displayName ?: "Dzuhur"
  val startClock =
    remember(sampleSlot, uiState.preferences.timeZoneId) {
      sampleSlot?.let {
        KemenagPrayerCalculator.formatClock(it.startEpochMillis, uiState.preferences.timeZoneId)
      } ?: "11:45"
    }
  val endClock =
    remember(sampleSlot, uiState.preferences.timeZoneId) {
      sampleSlot?.endEpochMillis?.let {
        KemenagPrayerCalculator.formatClock(it, uiState.preferences.timeZoneId)
      } ?: "15:01"
    }

  val activeSoundSetting =
    when (selectedMoment) {
      NotificationPreviewMoment.ENTRY -> prayerEntrySetting
      NotificationPreviewMoment.H30,
      NotificationPreviewMoment.H15 -> expiryReminderSetting
    }
  val hasAudibleSound =
    activeSoundSetting.mode == SoundMode.DEFAULT_ADZAN ||
      activeSoundSetting.mode == SoundMode.CUSTOM_FILE
  val isLoopingSound = hasAudibleSound && selectedMoment != NotificationPreviewMoment.ENTRY

  val previewTitle =
    when (selectedMoment) {
      NotificationPreviewMoment.ENTRY -> "$prayerName • $startClock"
      NotificationPreviewMoment.H30 -> "$prayerName • 30 mnt lagi"
      NotificationPreviewMoment.H15 -> "$prayerName • 15 mnt lagi!"
    }
  val previewBody =
    when (selectedMoment) {
      NotificationPreviewMoment.ENTRY -> "Masuk waktu • Habis $endClock"
      NotificationPreviewMoment.H30 -> "Batas waktu pukul $endClock"
      NotificationPreviewMoment.H15 -> "Segera shalat • Habis $endClock"
    }

  Card(
    modifier = modifier.testTag("notification_preview_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = accentColor.copy(alpha = 0.14f),
          modifier = Modifier.size(34.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Pratinjau Notifikasi Ringkas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              if (selectedMoment == NotificationPreviewMoment.ENTRY) {
                "Tema ${activeVisual.themeLabel} • Putar 1x sampai suara/azan habis"
              } else {
                "Tema ${activeVisual.themeLabel} • Berbunyi berulang hingga ditutup"
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Moment selector tabs (Masuk, H-30, H-15)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        NotificationPreviewMoment.entries.forEach { moment ->
          val selected = moment == selectedMoment
          Surface(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  selectedMoment = moment
                }
                .testTag("preview_moment_${moment.id}"),
            shape = RoundedCornerShape(10.dp),
            color =
              if (selected) accentColor
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = moment.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color =
                  if (selected) SoftInkOnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Minimalist 2-line Notification Banner Visual Mockup
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("notification_visual_mockup"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = previewTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = previewBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Box(
              modifier =
                Modifier.size(38.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(accentColor, Color(activeVisual.secondaryAccentColor))
                    )
                  ),
              contentAlignment = Alignment.Center,
            ) {
              if (activeVisual.mosqueDrawableRes != null) {
                Image(
                  painter = painterResource(id = activeVisual.mosqueDrawableRes),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Mosque,
                  contentDescription = null,
                  tint = SoftInkOnAccent,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.14f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "Sudah Shalat",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "Tutup",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = if (isLoopingSound) Icons.Rounded.Repeat else Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text =
                  when {
                    isLoopingSound ->
                      activeSoundSetting.customDisplayName ?: "Berulang • Mode Alarm"
                    hasAudibleSound ->
                      activeSoundSetting.customDisplayName?.let { "$it (1x Penuh)" }
                        ?: "1x Sampai Selesai • Alarm"
                    else -> activeSoundSetting.mode.label
                  },
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Real-device notification trigger button
      Button(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onSendLiveNotificationPreview(selectedMoment, activeSoundSetting)
        },
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .testTag("send_live_notification_preview_button"),
        shape = RoundedCornerShape(12.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.NotificationsActive,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Kirim Uji Notifikasi Asli (+ Suara)")
      }
    }
  }
}

@Composable
fun NotificationPreviewDialog(
  uiState: PrayerUiState,
  onSendLiveNotificationPreview: (NotificationPreviewMoment, SoundCategorySetting) -> Unit,
  onOpenSoundSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Preview Notifikasi Alarm") },
    text = {
      NotificationPreviewCard(
        uiState = uiState,
        prayerEntrySetting = uiState.preferences.prayerEntrySound,
        expiryReminderSetting = uiState.preferences.expiryReminderSound,
        onSendLiveNotificationPreview = onSendLiveNotificationPreview,
      )
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismiss()
          onOpenSoundSettings()
        }
      ) {
        Text("Atur Suara")
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
  )
}
