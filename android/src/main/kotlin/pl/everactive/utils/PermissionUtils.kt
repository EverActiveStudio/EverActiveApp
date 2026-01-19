package pl.everactive.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    const val PERMISSION_CODE_LOCATION = 100
    const val PERMISSION_CODE_ACTIVITY_RECOGNITION = 101
    const val PERMISSION_CODE_POST_NOTIFICATIONS = 102

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasActivityRecognitionPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED

    fun hasPostNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun allPermissionsGranted(context: Context): Boolean =
        hasLocationPermission(context) && hasActivityRecognitionPermission(context) && hasPostNotificationPermission(context)

    fun getRequiredPermissions(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.POST_NOTIFICATIONS
    )
}
