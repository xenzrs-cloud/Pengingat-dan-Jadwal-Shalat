package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.WidgetThemeMode
import java.util.Calendar
import java.util.TimeZone

enum class DynamicDayPhase {
  DAWN,
  DAY,
  SUNSET,
  NIGHT,
}

data class WidgetVisualStyle(
  @DrawableRes val backgroundImageRes: Int?,
  @DrawableRes val slotDefaultBgRes: Int,
  @DrawableRes val slotActiveBgRes: Int,
  @DrawableRes val refreshIconRes: Int,
  @DrawableRes val paletteIconRes: Int,
  val primaryTextColor: Int,
  val secondaryTextColor: Int,
  val accentTextColor: Int,
  val imsakLabelColor: Int,
)

object WidgetThemeHelper {

  fun nextMode(current: WidgetThemeMode): WidgetThemeMode {
    val all = WidgetThemeMode.entries
    val nextIdx = (all.indexOf(current) + 1) % all.size
    return all[nextIdx]
  }

  fun renderBackgroundBitmap(
    context: Context,
    @DrawableRes resId: Int,
    sizePx: Int = 256,
  ): Bitmap? =
    runCatching {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return@runCatching null
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
      }
      .getOrNull()

  fun resolveDynamicPhase(nowMillis: Long, timeZoneId: String): DynamicDayPhase {
    val cal =
      Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply { timeInMillis = nowMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val totalMinutes = hour * 60 + minute
    return when {
      totalMinutes in (4 * 60) until (6 * 60) -> DynamicDayPhase.DAWN
      totalMinutes in (6 * 60) until (15 * 60) -> DynamicDayPhase.DAY
      totalMinutes in (15 * 60) until (18 * 60 + 30) -> DynamicDayPhase.SUNSET
      else -> DynamicDayPhase.NIGHT
    }
  }

  fun resolveStyle(
    context: Context,
    mode: WidgetThemeMode,
  ): WidgetVisualStyle {
    if (mode == WidgetThemeMode.DEFAULT) {
      return WidgetVisualStyle(
        backgroundImageRes = null,
        slotDefaultBgRes = R.drawable.widget_slot_default_bg,
        slotActiveBgRes = R.drawable.widget_slot_active_bg,
        refreshIconRes = R.drawable.ic_widget_refresh,
        paletteIconRes = R.drawable.ic_widget_palette,
        primaryTextColor = ContextCompat.getColor(context, R.color.widget_text_primary),
        secondaryTextColor = ContextCompat.getColor(context, R.color.widget_text_secondary),
        accentTextColor = ContextCompat.getColor(context, R.color.widget_primary),
        imsakLabelColor = ContextCompat.getColor(context, R.color.widget_imsak_accent),
      )
    }

    val phase =
      when (mode) {
        WidgetThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
        WidgetThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
        WidgetThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
        WidgetThemeMode.DEFAULT -> DynamicDayPhase.DAY
      }

    return when (phase) {
      DynamicDayPhase.DAY ->
        WidgetVisualStyle(
          backgroundImageRes = R.drawable.bg_widget_mosque_day,
          slotDefaultBgRes = R.drawable.widget_slot_glass_light,
          slotActiveBgRes = R.drawable.widget_slot_glass_light_active,
          refreshIconRes = R.drawable.ic_widget_refresh,
          paletteIconRes = R.drawable.ic_widget_palette,
          primaryTextColor = Color.parseColor("#0F172A"),
          secondaryTextColor = Color.parseColor("#334155"),
          accentTextColor = Color.parseColor("#0F766E"),
          imsakLabelColor = Color.parseColor("#B45309"),
        )
      DynamicDayPhase.DAWN ->
        WidgetVisualStyle(
          backgroundImageRes = R.drawable.bg_widget_mosque_dawn,
          slotDefaultBgRes = R.drawable.widget_slot_glass_dark,
          slotActiveBgRes = R.drawable.widget_slot_glass_dark_active,
          refreshIconRes = R.drawable.ic_widget_refresh_white,
          paletteIconRes = R.drawable.ic_widget_palette_white,
          primaryTextColor = Color.WHITE,
          secondaryTextColor = Color.parseColor("#E2E8F0"),
          accentTextColor = Color.parseColor("#FDE047"),
          imsakLabelColor = Color.parseColor("#FDE68A"),
        )
      DynamicDayPhase.SUNSET ->
        WidgetVisualStyle(
          backgroundImageRes = R.drawable.bg_widget_mosque_sunset,
          slotDefaultBgRes = R.drawable.widget_slot_glass_dark,
          slotActiveBgRes = R.drawable.widget_slot_glass_dark_active,
          refreshIconRes = R.drawable.ic_widget_refresh_white,
          paletteIconRes = R.drawable.ic_widget_palette_white,
          primaryTextColor = Color.WHITE,
          secondaryTextColor = Color.parseColor("#FEF3C7"),
          accentTextColor = Color.parseColor("#FDE047"),
          imsakLabelColor = Color.parseColor("#FDE68A"),
        )
      DynamicDayPhase.NIGHT ->
        WidgetVisualStyle(
          backgroundImageRes = R.drawable.bg_widget_mosque_night,
          slotDefaultBgRes = R.drawable.widget_slot_glass_dark,
          slotActiveBgRes = R.drawable.widget_slot_glass_dark_active,
          refreshIconRes = R.drawable.ic_widget_refresh_white,
          paletteIconRes = R.drawable.ic_widget_palette_white,
          primaryTextColor = Color.WHITE,
          secondaryTextColor = Color.parseColor("#CBD5E1"),
          accentTextColor = Color.parseColor("#FDE047"),
          imsakLabelColor = Color.parseColor("#FDE047"),
        )
    }
  }
}
