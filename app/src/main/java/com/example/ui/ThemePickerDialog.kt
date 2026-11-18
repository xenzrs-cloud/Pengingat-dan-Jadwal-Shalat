package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.WidgetThemeMode
import com.example.widget.DynamicDayPhase

@Composable
fun ThemePickerDialog(
  selectedMode: WidgetThemeMode,
  currentPhase: DynamicDayPhase,
  onSelectMode: (WidgetThemeMode) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Palette,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text("Tema Aplikasi & Widget")
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        item {
          Text(
            text =
              "Pilih tampilan warna & latar masjid untuk aplikasi sekaligus widget layar utama.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        items(WidgetThemeMode.entries.size) { idx ->
          val mode = WidgetThemeMode.entries[idx]
          val isSelected = mode == selectedMode
          val previewRes =
            when (mode) {
              WidgetThemeMode.DEFAULT -> null
              WidgetThemeMode.DYNAMIC_MOSQUE ->
                when (currentPhase) {
                  DynamicDayPhase.DAWN -> R.drawable.bg_widget_mosque_dawn
                  DynamicDayPhase.DAY -> R.drawable.bg_widget_mosque_day
                  DynamicDayPhase.SUNSET -> R.drawable.bg_widget_mosque_sunset
                  DynamicDayPhase.NIGHT -> R.drawable.bg_widget_mosque_night
                }
              WidgetThemeMode.MOSQUE_DAY -> R.drawable.bg_widget_mosque_day
              WidgetThemeMode.MOSQUE_SUNSET -> R.drawable.bg_widget_mosque_sunset
              WidgetThemeMode.MOSQUE_NIGHT -> R.drawable.bg_widget_mosque_night
            }

          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .selectable(
                  selected = isSelected,
                  onClick = { onSelectMode(mode) },
                  role = Role.RadioButton,
                )
                .testTag("app_theme_option_${mode.id}"),
            shape = RoundedCornerShape(14.dp),
            color =
              if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border =
              if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
              else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F766E)),
                contentAlignment = Alignment.Center,
              ) {
                if (previewRes != null) {
                  Image(
                    painter = painterResource(id = previewRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = mode.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
