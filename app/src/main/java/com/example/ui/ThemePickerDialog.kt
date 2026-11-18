package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AppThemeMode
import com.example.ui.theme.SoftInkOnAccent
import com.example.ui.theme.resolveColorScheme
import com.example.widget.DynamicDayPhase

@Composable
fun ThemePickerDialog(
  selectedMode: AppThemeMode,
  currentPhase: DynamicDayPhase,
  onSelectMode: (AppThemeMode) -> Unit,
  onDismiss: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val isDark = isSystemInDarkTheme()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column {
          Text("Tema Tampilan Aplikasi")
          Text(
            text = "Khusus tampilan aplikasi (widget punya tema terpisah)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(380.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        items(AppThemeMode.entries.size) { idx ->
          val mode = AppThemeMode.entries[idx]
          val isSelected = mode == selectedMode
          val previewScheme =
            resolveColorScheme(
              darkTheme = isDark,
              themeMode = mode,
              dynamicPhase = currentPhase,
            )
          val previewRes =
            when (mode) {
              AppThemeMode.DEFAULT -> null
              AppThemeMode.DYNAMIC_MOSQUE ->
                when (currentPhase) {
                  DynamicDayPhase.DAWN -> R.drawable.bg_widget_mosque_dawn
                  DynamicDayPhase.DAY -> R.drawable.bg_widget_mosque_day
                  DynamicDayPhase.SUNSET -> R.drawable.bg_widget_mosque_sunset
                  DynamicDayPhase.NIGHT -> R.drawable.bg_widget_mosque_night
                }
              AppThemeMode.MOSQUE_DAY -> R.drawable.bg_widget_mosque_day
              AppThemeMode.MOSQUE_SUNSET -> R.drawable.bg_widget_mosque_sunset
              AppThemeMode.MOSQUE_NIGHT -> R.drawable.bg_widget_mosque_night
            }

          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .selectable(
                  selected = isSelected,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectMode(mode)
                  },
                  role = Role.RadioButton,
                )
                .testTag("app_theme_option_${mode.id}"),
            shape = RoundedCornerShape(16.dp),
            color =
              if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border =
              if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
              else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                      Brush.linearGradient(
                        listOf(previewScheme.primary, previewScheme.tertiary)
                      )
                    ),
                contentAlignment = Alignment.Center,
              ) {
                if (previewRes != null) {
                  Image(
                    painter = painterResource(id = previewRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = SoftInkOnAccent,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = "Terpilih",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
                Text(
                  text = mode.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Color palette swatch preview
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  SwatchDot(color = previewScheme.primary, border = MaterialTheme.colorScheme.outlineVariant)
                  SwatchDot(color = previewScheme.secondary, border = MaterialTheme.colorScheme.outlineVariant)
                  SwatchDot(color = previewScheme.background, border = MaterialTheme.colorScheme.outline)
                }
              }

              RadioButton(selected = isSelected, onClick = null)
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
private fun SwatchDot(color: Color, border: Color) {
  Surface(
    modifier = Modifier.size(12.dp),
    shape = CircleShape,
    color = color,
    border = BorderStroke(1.dp, border),
  ) {}
}
