package com.example.volkan.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.volkan.config.AppConfig
import com.example.volkan.util.OriginValidator
import com.example.volkan.util.VolkanLogger

class PermissionManager(
    private val activity: ComponentActivity,
    private val appConfig: AppConfig
) {
    private var pendingWebPermissionCallback: ((Array<String>) -> Unit)? = null
    private var pendingCandidateResources: Array<String> = emptyArray()

    private var pendingLocationCallback: ((Boolean) -> Unit)? = null
    private var pendingNotificationCallback: ((Boolean) -> Unit)? = null

    private val multiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // 1. Process pending Web Audio/Video capture permissions
            if (pendingWebPermissionCallback != null) {
                val grantedWebResources = mutableListOf<String>()
                for (res in pendingCandidateResources) {
                    when (res) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            if (results[Manifest.permission.CAMERA] == true ||
                                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            ) {
                                grantedWebResources.add(res)
                            }
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            if (results[Manifest.permission.RECORD_AUDIO] == true ||
                                ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            ) {
                                grantedWebResources.add(res)
                            }
                        }
                    }
                }
                pendingWebPermissionCallback?.invoke(grantedWebResources.toTypedArray())
                pendingWebPermissionCallback = null
                pendingCandidateResources = emptyArray()
            }

            // 2. Process pending Geolocation request
            if (pendingLocationCallback != null) {
                val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val isLocationGranted = fineGranted || coarseGranted
                pendingLocationCallback?.invoke(isLocationGranted)
                pendingLocationCallback = null
            }

            // 3. Process pending Notification request
            if (pendingNotificationCallback != null) {
                val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    results[Manifest.permission.POST_NOTIFICATIONS] == true ||
                            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                pendingNotificationCallback?.invoke(notificationGranted)
                pendingNotificationCallback = null
            }
        }

    fun requestWebPermissions(
        origin: String?,
        resources: Array<String>,
        onResult: (Array<String>) -> Unit
    ) {
        // 1. Strict origin validation
        if (!OriginValidator.isTrustedAppOrigin(origin, appConfig)) {
            VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Rejecting permission request from untrusted origin: $origin")
            onResult(emptyArray())
            return
        }

        // 2. Filter requested resources based on capability configuration
        val candidateWebResources = mutableListOf<String>()
        val requiredAndroidPermissions = mutableListOf<String>()

        for (res in resources) {
            when (res) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    if (appConfig.cameraEnabled) {
                        candidateWebResources.add(res)
                        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requiredAndroidPermissions.add(Manifest.permission.CAMERA)
                        }
                    } else {
                        VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Camera resource requested but cameraEnabled is false in app-config")
                    }
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    if (appConfig.microphoneEnabled) {
                        candidateWebResources.add(res)
                        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requiredAndroidPermissions.add(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Microphone resource requested but microphoneEnabled is false in app-config")
                    }
                }
            }
        }

        if (candidateWebResources.isEmpty()) {
            onResult(emptyArray())
            return
        }

        if (requiredAndroidPermissions.isEmpty()) {
            // All needed Android permissions are already granted
            onResult(candidateWebResources.toTypedArray())
        } else {
            pendingCandidateResources = candidateWebResources.toTypedArray()
            pendingWebPermissionCallback = onResult
            multiplePermissionsLauncher.launch(requiredAndroidPermissions.toTypedArray())
        }
    }

    fun requestGeolocationPermission(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin == null || callback == null) return

        // 1. Strict origin validation
        if (!OriginValidator.isTrustedAppOrigin(origin, appConfig)) {
            VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Geolocation denied: Untrusted origin $origin")
            callback.invoke(origin, false, false)
            return
        }

        // 2. Capability check
        if (!appConfig.geolocationEnabled) {
            VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Geolocation denied: geolocationEnabled is false in app-config")
            callback.invoke(origin, false, false)
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            callback.invoke(origin, true, false)
            return
        }

        // Request runtime permission
        pendingLocationCallback = { granted ->
            activity.runOnUiThread {
                if (granted) {
                    VolkanLogger.i(VolkanLogger.TAG_PERMISSION, "Geolocation granted for origin: $origin")
                    callback.invoke(origin, true, false)
                } else {
                    VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Geolocation runtime permission denied by user")
                    callback.invoke(origin, false, false)
                }
            }
        }

        multiplePermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (!appConfig.notificationsEnabled) {
            VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Notification request rejected: notificationsEnabled is false")
            onResult(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onResult(true)
            } else {
                pendingNotificationCallback = onResult
                multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        } else {
            onResult(true)
        }
    }
}
