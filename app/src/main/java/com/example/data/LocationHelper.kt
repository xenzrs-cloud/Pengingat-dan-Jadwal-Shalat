package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.domain.IndonesianCities
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GpsLocationResult(
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String,
  val displayLabel: String,
)

object LocationHelper {

  fun hasLocationPermission(context: Context): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(context: Context): GpsLocationResult? {
    if (!hasLocationPermission(context)) return null

    // 1. Try FusedLocationProviderClient current location (with 6s timeout)
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val currentFused =
      withTimeoutOrNull(6_000L) {
        suspendCancellableCoroutine<Location?> { cont ->
          val cts = CancellationTokenSource()
          cont.invokeOnCancellation { cts.cancel() }
          runCatching {
              fusedClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                  if (cont.isActive) cont.resume(loc)
                }
                .addOnFailureListener {
                  if (cont.isActive) cont.resume(null)
                }
                .addOnCanceledListener {
                  if (cont.isActive) cont.resume(null)
                }
            }
            .onFailure { if (cont.isActive) cont.resume(null) }
        }
      }

    val location =
      currentFused
        ?: fetchLastFusedLocation(context)
        ?: fetchLastPlatformLocation(context)
        ?: return null

    return toResult(location.latitude, location.longitude)
  }

  @SuppressLint("MissingPermission")
  private suspend fun fetchLastFusedLocation(context: Context): Location? {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    return withTimeoutOrNull(2_000L) {
      suspendCancellableCoroutine { cont ->
        runCatching {
            fusedClient.lastLocation
              .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
              .addOnFailureListener { if (cont.isActive) cont.resume(null) }
          }
          .onFailure { if (cont.isActive) cont.resume(null) }
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun fetchLastPlatformLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers
      .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
      .maxByOrNull { it.time }
  }

  private fun toResult(latitude: Double, longitude: Double): GpsLocationResult {
    val nearest = IndonesianCities.findNearestCityName(latitude, longitude)
    val timeZoneId = IndonesianCities.inferTimeZoneId(longitude)
    val coordText = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
    val label =
      if (nearest != null) {
        val distSq =
          (nearest.latitude - latitude) * (nearest.latitude - latitude) +
            (nearest.longitude - longitude) * (nearest.longitude - longitude)
        if (distSq < 0.25) "GPS • ${nearest.name}" else "GPS • $coordText"
      } else {
        "GPS • $coordText"
      }
    return GpsLocationResult(
      latitude = latitude,
      longitude = longitude,
      timeZoneId = timeZoneId,
      displayLabel = label,
    )
  }
}
