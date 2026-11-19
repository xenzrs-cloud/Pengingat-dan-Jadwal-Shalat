package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.alarm.NotificationPreviewMoment
import com.example.domain.SoundCategory
import com.example.domain.SoundCategorySetting
import com.example.domain.SoundMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
  uiState: PrayerUiState,
  onNavigateBack: () -> Unit,
  onSelectMode: (SoundCategory, SoundMode) -> Unit,
  onPickAudioFile: (SoundCategory, Uri) -> Unit,
  onTestSound: (SoundCategory) -> Unit,
  onStopSound: () -> Unit,
  onSendLiveNotificationPreview: (NotificationPreviewMoment, SoundCategorySetting) -> Unit,
  onSaveAndApply: () -> Unit,
  onSnackbarShown: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val haptics = LocalHapticFeedback.current
  var pendingPickerCategory by remember { mutableStateOf<SoundCategory?>(null) }

  val hasUnsavedChanges =
    uiState.draftPrayerEntry != uiState.preferences.prayerEntrySound ||
      uiState.draftImsakEntry != uiState.preferences.imsakEntrySound ||
      uiState.draftExpiryReminder != uiState.preferences.expiryReminderSound

  val audioPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      val targetCategory = pendingPickerCategory
      if (uri != null && targetCategory != null) {
        onPickAudioFile(targetCategory, uri)
      }
      pendingPickerCategory = null
    }

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      onSnackbarShown()
    }
  }

  val primaryTint = MaterialTheme.colorScheme.primary
  val backgroundColor = MaterialTheme.colorScheme.background

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = backgroundColor,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.nav_sound_settings),
              style = MaterialTheme.typography.titleLarge,
            )
            if (hasUnsavedChanges) {
              Text(
                text = "Perubahan belum diterapkan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              onStopSound()
              onNavigateBack()
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("back_from_sound_settings_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AnimatedVisibility(visible = hasUnsavedChanges) {
            Text(
              text = "Tekan Simpan untuk memperbarui Channel Notifikasi & jadwal alarm.",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
          Button(
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              onSaveAndApply()
            },
            modifier =
              Modifier.widthIn(max = 600.dp)
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_sound_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                if (hasUnsavedChanges) "Simpan & Terapkan Perubahan"
                else stringResource(R.string.action_save_apply),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize().drawWithCache {
          val atmosphere =
            Brush.verticalGradient(
              colors =
                listOf(
                  primaryTint.copy(alpha = 0.08f),
                  backgroundColor,
                )
            )
          onDrawBehind { drawRect(atmosphere) }
        }
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        item {
          ChannelVersionNoticeCard(
            channelVersion = uiState.preferences.channelVersion,
            hasUnsavedChanges = hasUnsavedChanges,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }

        item {
          NotificationPreviewCard(
            uiState = uiState,
            prayerEntrySetting = uiState.draftPrayerEntry,
            expiryReminderSetting = uiState.draftExpiryReminder,
            onSendLiveNotificationPreview = onSendLiveNotificationPreview,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }

        item {
          SoundCategoryCard(
            category = SoundCategory.PRAYER_ENTRY,
            setting = uiState.draftPrayerEntry,
            isPlaying = uiState.playingPreviewCategory == SoundCategory.PRAYER_ENTRY,
            onSelectMode = { mode ->
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectMode(SoundCategory.PRAYER_ENTRY, mode)
            },
            onLaunchFilePicker = {
              pendingPickerCategory = SoundCategory.PRAYER_ENTRY
              audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onTestSound = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTestSound(SoundCategory.PRAYER_ENTRY)
            },
            onStopSound = onStopSound,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }

        item {
          SoundCategoryCard(
            category = SoundCategory.IMSAK_ENTRY,
            setting = uiState.draftImsakEntry,
            isPlaying = uiState.playingPreviewCategory == SoundCategory.IMSAK_ENTRY,
            onSelectMode = { mode ->
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectMode(SoundCategory.IMSAK_ENTRY, mode)
            },
            onLaunchFilePicker = {
              pendingPickerCategory = SoundCategory.IMSAK_ENTRY
              audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onTestSound = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTestSound(SoundCategory.IMSAK_ENTRY)
            },
            onStopSound = onStopSound,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }

        item {
          SoundCategoryCard(
            category = SoundCategory.EXPIRY_REMINDER,
            setting = uiState.draftExpiryReminder,
            isPlaying = uiState.playingPreviewCategory == SoundCategory.EXPIRY_REMINDER,
            onSelectMode = { mode ->
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectMode(SoundCategory.EXPIRY_REMINDER, mode)
            },
            onLaunchFilePicker = {
              pendingPickerCategory = SoundCategory.EXPIRY_REMINDER
              audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onTestSound = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTestSound(SoundCategory.EXPIRY_REMINDER)
            },
            onStopSound = onStopSound,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun ChannelVersionNoticeCard(
  channelVersion: Int,
  hasUnsavedChanges: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Channel Notifikasi Aktif: v$channelVersion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          if (hasUnsavedChanges) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.secondary,
            ) {
              Text(
                text = "Draft",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(
          text =
            "Menekan Simpan akan memperbarui versi Notification Channel Android, menghapus versi lama, dan menjadwalkan ulang seluruh alarm (termasuk Imsak).",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
        )
      }
    }
  }
}

@Composable
private fun SoundCategoryCard(
  category: SoundCategory,
  setting: SoundCategorySetting,
  isPlaying: Boolean,
  onSelectMode: (SoundMode) -> Unit,
  onLaunchFilePicker: () -> Unit,
  onTestSound: () -> Unit,
  onStopSound: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = category.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = category.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      SoundMode.entries.forEach { mode ->
        val selected = setting.mode == mode
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .selectable(
                selected = selected,
                onClick = { onSelectMode(mode) },
                role = Role.RadioButton,
              )
              .testTag("sound_mode_${category.id}_${mode.id}"),
          shape = RoundedCornerShape(12.dp),
          color =
            if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border =
            if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector =
                when (mode) {
                  SoundMode.DEFAULT_ADZAN -> Icons.Rounded.VolumeUp
                  SoundMode.CUSTOM_FILE -> Icons.Rounded.AudioFile
                  SoundMode.VIBRATE_ONLY -> Icons.Rounded.Vibration
                  SoundMode.SILENT -> Icons.Rounded.VolumeOff
                },
              contentDescription = null,
              tint =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = mode.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              )
              Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = setting.mode == SoundMode.CUSTOM_FILE,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "File Audio Terpilih:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = setting.customDisplayName ?: "Belum ada file audio dipilih",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            FilledTonalButton(
              onClick = onLaunchFilePicker,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("pick_audio_button_${category.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.action_pick_audio))
            }
          }
        }
      }

      // Preview & Stop Audio Controls
      AnimatedVisibility(
        visible = setting.mode == SoundMode.DEFAULT_ADZAN || setting.mode == SoundMode.CUSTOM_FILE,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = onTestSound,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("test_sound_button_${category.id}"),
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
              contentDescription = stringResource(R.string.cd_play_sound),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isPlaying) "Memutar..." else stringResource(R.string.action_test_sound)
            )
          }

          OutlinedButton(
            onClick = onStopSound,
            enabled = isPlaying,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("stop_sound_button_${category.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Stop,
              contentDescription = stringResource(R.string.cd_stop_sound),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.action_stop_sound))
          }
        }
      }
    }
  }
}
