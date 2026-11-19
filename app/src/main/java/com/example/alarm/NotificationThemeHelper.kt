package com.example.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.AppThemeMode
import com.example.domain.PrayerType
import com.example.widget.DynamicDayPhase
import com.example.widget.WidgetThemeHelper

data class NotificationThemeVisual(
  val accentColor: Int,
  val secondaryAccentColor: Int,
  val themeLabel: String,
  @DrawableRes val mosqueDrawableRes: Int?,
)

enum class NotificationPreviewMoment(val id: String, val label: String, val badgeText: String) {
  ENTRY("entry", "Masuk", "Masuk Waktu"),
  H30("h30", "H-30", "H-30 Menit"),
  H15("h15", "H-15", "H-15 Segera"),
}

object NotificationThemeHelper {

  fun resolveVisual(
    mode: AppThemeMode,
    nowMillis: Long = System.currentTimeMillis(),
    timeZoneId: String = "Asia/Jakarta",
  ): NotificationThemeVisual {
    if (mode == AppThemeMode.DEFAULT) {
      return NotificationThemeVisual(
        accentColor = Color.parseColor("#0F766E"),
        secondaryAccentColor = Color.parseColor("#14B8A6"),
        themeLabel = "Default",
        mosqueDrawableRes = null,
      )
    }

    val phase =
      when (mode) {
        AppThemeMode.DYNAMIC_MOSQUE -> WidgetThemeHelper.resolveDynamicPhase(nowMillis, timeZoneId)
        AppThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
        AppThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
        AppThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
        AppThemeMode.DEFAULT -> DynamicDayPhase.DAY
      }

    return when (phase) {
      DynamicDayPhase.DAWN ->
        NotificationThemeVisual(
          accentColor = Color.parseColor("#5B21B6"),
          secondaryAccentColor = Color.parseColor("#BE185D"),
          themeLabel = "Fajar",
          mosqueDrawableRes = R.drawable.bg_widget_mosque_dawn,
        )
      DynamicDayPhase.DAY ->
        NotificationThemeVisual(
          accentColor = Color.parseColor("#0284C7"),
          secondaryAccentColor = Color.parseColor("#0F766E"),
          themeLabel = "Siang",
          mosqueDrawableRes = R.drawable.bg_widget_mosque_day,
        )
      DynamicDayPhase.SUNSET ->
        NotificationThemeVisual(
          accentColor = Color.parseColor("#EA580C"),
          secondaryAccentColor = Color.parseColor("#BE185D"),
          themeLabel = "Senja",
          mosqueDrawableRes = R.drawable.bg_widget_mosque_sunset,
        )
      DynamicDayPhase.NIGHT ->
        NotificationThemeVisual(
          accentColor = Color.parseColor("#0F766E"),
          secondaryAccentColor = Color.parseColor("#FACC15"),
          themeLabel = "Malam",
          mosqueDrawableRes = R.drawable.bg_widget_mosque_night,
        )
    }
  }

  fun renderLargeIconBitmap(
    context: Context,
    visual: NotificationThemeVisual,
    sizePx: Int = 96,
  ): Bitmap? =
    runCatching {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx / 2f
        val clipPath = Path().apply { addCircle(radius, radius, radius, Path.Direction.CW) }
        canvas.clipPath(clipPath)

        val resId = visual.mosqueDrawableRes
        val drawable = resId?.let { ContextCompat.getDrawable(context, it) }
        if (drawable != null) {
          drawable.setBounds(0, 0, sizePx, sizePx)
          drawable.draw(canvas)
        } else {
          val bgPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              shader =
                LinearGradient(
                  0f,
                  0f,
                  sizePx.toFloat(),
                  sizePx.toFloat(),
                  visual.accentColor,
                  visual.secondaryAccentColor,
                  Shader.TileMode.CLAMP,
                )
            }
          canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

          val domePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
          val domeBounds =
            RectF(sizePx * 0.24f, sizePx * 0.30f, sizePx * 0.76f, sizePx * 0.80f)
          canvas.drawArc(domeBounds, 180f, 180f, true, domePaint)
          canvas.drawRect(sizePx * 0.24f, sizePx * 0.55f, sizePx * 0.76f, sizePx * 0.82f, domePaint)
          canvas.drawCircle(sizePx * 0.50f, sizePx * 0.24f, sizePx * 0.05f, domePaint)
        }

        bitmap
      }
      .getOrNull()

  fun buildAlarmNotification(
    context: Context,
    notificationId: Int,
    channelId: String,
    title: String,
    body: String,
    isUrgent: Boolean,
    appThemeMode: AppThemeMode,
    timeZoneId: String,
    prayerType: PrayerType? = null,
    year: Int = 0,
    month: Int = 0,
    day: Int = 0,
    isTestPreview: Boolean = false,
    isInsistent: Boolean = true,
  ): Notification {
    val visual =
      resolveVisual(
        mode = appThemeMode,
        nowMillis = System.currentTimeMillis(),
        timeZoneId = timeZoneId,
      )
    val largeIcon = renderLargeIconBitmap(context, visual, sizePx = 96)

    val openAppIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val contentPendingIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      Intent(context, PrayerAlarmReceiver::class.java).apply {
        action = PrayerAlarmReceiver.ACTION_STOP_ALARM
        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
      }
    val stopPendingIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId + 50_000,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val builder =
      NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setColor(visual.accentColor)
        .setPriority(
          if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
        )
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(false)
        .setAutoCancel(false)
        .setContentIntent(contentPendingIntent)
        .setDeleteIntent(stopPendingIntent)

    if (largeIcon != null) {
      builder.setLargeIcon(largeIcon)
    }

    if (isTestPreview) {
      if (prayerType == null || prayerType.isObligatory) {
        builder.addAction(0, "Sudah Shalat (Uji)", stopPendingIntent)
      }
    } else if (prayerType != null && prayerType.isObligatory && year > 0) {
      val markPrayedIntent =
        Intent(context, PrayerAlarmReceiver::class.java).apply {
          action = PrayerAlarmReceiver.ACTION_MARK_ALREADY_PRAYED
          putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
          putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayerType.id)
          putExtra(PrayerAlarmReceiver.EXTRA_YEAR, year)
          putExtra(PrayerAlarmReceiver.EXTRA_MONTH, month)
          putExtra(PrayerAlarmReceiver.EXTRA_DAY, day)
        }
      val markPrayedPending =
        PendingIntent.getBroadcast(
          context,
          notificationId + 60_000,
          markPrayedIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      builder.addAction(0, "Sudah Shalat", markPrayedPending)
    }

    builder.addAction(0, "Tutup", stopPendingIntent)

    val notification = builder.build()
    if (isInsistent) {
      notification.flags = notification.flags or Notification.FLAG_INSISTENT
    }
    return notification
  }

  fun buildAndShowNotification(
    context: Context,
    notificationId: Int,
    channelId: String,
    title: String,
    body: String,
    isUrgent: Boolean,
    appThemeMode: AppThemeMode,
    timeZoneId: String,
    prayerType: PrayerType? = null,
    year: Int = 0,
    month: Int = 0,
    day: Int = 0,
    isTestPreview: Boolean = false,
    isInsistent: Boolean = true,
  ): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }

    val notification =
      buildAlarmNotification(
        context = context,
        notificationId = notificationId,
        channelId = channelId,
        title = title,
        body = body,
        isUrgent = isUrgent,
        appThemeMode = appThemeMode,
        timeZoneId = timeZoneId,
        prayerType = prayerType,
        year = year,
        month = month,
        day = day,
        isTestPreview = isTestPreview,
        isInsistent = isInsistent,
      )

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(notificationId, notification)
    return true
  }
}
