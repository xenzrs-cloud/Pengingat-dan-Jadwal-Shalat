package com.example

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.LocationHelper
import com.example.ui.PrayerViewModel
import com.example.ui.ScheduleScreen
import com.example.ui.SoundSettingsScreen
import com.example.ui.WidgetStyleOption
import com.example.ui.theme.PengingatShalatTheme
import com.example.widget.MediumPrayerWidgetProvider
import com.example.widget.PrayerScheduleWidgetProvider
import com.example.widget.WidePrayerWidgetProvider
import com.example.widget.WidgetThemeHelper

object AppRoutes {
  const val ROUTE_SCHEDULE = "schedule"
  const val ROUTE_SOUND_SETTINGS = "sound_settings"
}

class MainActivity : ComponentActivity() {

  private var prayerViewModel: PrayerViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val vm: PrayerViewModel = viewModel()
      prayerViewModel = vm
      val uiState by vm.uiState.collectAsStateWithLifecycle()
      val dynamicPhase =
        WidgetThemeHelper.resolveDynamicPhase(
          nowMillis = uiState.nowMillis,
          timeZoneId = uiState.preferences.timeZoneId,
        )

      PengingatShalatTheme(
        themeMode = uiState.preferences.appThemeMode,
        dynamicPhase = dynamicPhase,
      ) {
        val navController = rememberNavController()

        var hasNotificationPermission by remember {
          mutableStateOf(checkNotificationPermission(context))
        }

        val notificationPermissionLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { granted ->
            hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
          }

        val locationPermissionLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
          ) { permissions ->
            val granted =
              permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
              vm.refreshGpsLocation()
            }
          }

        LaunchedEffect(Unit) {
          if (LocationHelper.hasLocationPermission(context)) {
            vm.refreshGpsLocation()
          } else {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
              )
            )
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }

        NavHost(
          navController = navController,
          startDestination = AppRoutes.ROUTE_SCHEDULE,
        ) {
          composable(AppRoutes.ROUTE_SCHEDULE) {
            ScheduleScreen(
              uiState = uiState,
              hasNotificationPermission = hasNotificationPermission,
              onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              },
              onRequestExactAlarmSettings = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  val intent =
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                      data = Uri.parse("package:$packageName")
                    }
                  runCatching { startActivity(intent) }
                }
              },
              onRefreshGps = {
                if (LocationHelper.hasLocationPermission(context)) {
                  vm.refreshGpsLocation()
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_COARSE_LOCATION,
                      Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                  )
                }
              },
              onSelectCity = { city -> vm.selectCity(city) },
              onOpenSoundSettings = {
                vm.syncDraftFromSaved()
                navController.navigate(AppRoutes.ROUTE_SOUND_SETTINGS)
              },
              onSelectAppThemeMode = { mode -> vm.setAppThemeMode(mode) },
              onSelectWidgetThemeMode = { mode -> vm.setWidgetThemeMode(mode) },
              onPinWidgetStyle = { styleOption ->
                val targetClass =
                  when (styleOption) {
                    WidgetStyleOption.SIZE_2X2 -> PrayerScheduleWidgetProvider::class.java
                    WidgetStyleOption.SIZE_3X2 -> MediumPrayerWidgetProvider::class.java
                    WidgetStyleOption.SIZE_4X2 -> WidePrayerWidgetProvider::class.java
                  }
                val label =
                  when (styleOption) {
                    WidgetStyleOption.SIZE_2X2 -> "Jadwal Shalat (2x2)"
                    WidgetStyleOption.SIZE_3X2 -> "Jadwal & Berikutnya (3x2)"
                    WidgetStyleOption.SIZE_4X2 -> "Jadwal Lengkap (4x2)"
                  }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  val manager = AppWidgetManager.getInstance(context)
                  val provider = ComponentName(context, targetClass)
                  if (manager.isRequestPinAppWidgetSupported) {
                    val callbackIntent =
                      Intent(context, targetClass).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                      }
                    val successCallback =
                      PendingIntent.getBroadcast(
                        context,
                        9500 + styleOption.ordinal,
                        callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                      )
                    val requested =
                      runCatching { manager.requestPinAppWidget(provider, null, successCallback) }
                        .getOrDefault(false)
                    if (requested) {
                      vm.showSnackbar("Permintaan pasang widget $label dikirim ke layar utama.")
                    } else {
                      vm.showSnackbar(
                        "Launcher HP menolak pin otomatis. Aktifkan izin 'Pintasan layar utama' di Info Aplikasi."
                      )
                    }
                  } else {
                    vm.showSnackbar(
                      "Launcher HP ini tidak mendukung pin otomatis. Tekan lama layar utama → Widget → $label."
                    )
                  }
                } else {
                  vm.showSnackbar(
                    "Tekan lama layar utama HP lalu pilih Widget → $label."
                  )
                }
              },
              onOpenAppSettings = {
                val intent =
                  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                  }
                runCatching { startActivity(intent) }
              },
              onToggleAlreadyPrayed = { prayerType, checked ->
                vm.toggleAlreadyPrayed(prayerType, checked)
              },
              onSelectQuickOverride = { prayerType, override ->
                vm.setQuickOverride(prayerType, override)
              },
              onSnackbarShown = { vm.clearSnackbar() },
            )
          }

          composable(AppRoutes.ROUTE_SOUND_SETTINGS) {
            SoundSettingsScreen(
              uiState = uiState,
              onNavigateBack = { navController.popBackStack() },
              onSelectMode = { category, mode -> vm.updateDraftMode(category, mode) },
              onPickAudioFile = { category, uri -> vm.onAudioFilePicked(category, uri) },
              onTestSound = { category -> vm.previewDraftSound(category) },
              onStopSound = { vm.stopPreviewSound() },
              onSaveAndApply = { vm.saveAndApplySoundSettings() },
              onSnackbarShown = { vm.clearSnackbar() },
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    prayerViewModel?.refreshExactAlarmStatus()
  }

  private fun checkNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
  }
}
