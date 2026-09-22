package com.example.volkan.config

import android.content.Context
import com.example.volkan.util.VolkanLogger
import org.json.JSONArray
import org.json.JSONObject

enum class OrientationMode {
    PORTRAIT,
    LANDSCAPE,
    SENSOR,
    USER,
    UNSPECIFIED
}

enum class FullscreenMode {
    NORMAL,
    IMMERSIVE_STICKY,
    EDGE_TO_EDGE
}

enum class ExternalLinkPolicy {
    OPEN_IN_EXTERNAL_BROWSER,
    ALLOW_LIST_ONLY,
    BLOCK
}

enum class MultipleWindowPolicy {
    BLOCK,
    ALLOW_SAME_ORIGIN,
    OPEN_EXTERNAL
}

enum class BackButtonPolicy {
    WEB_HISTORY_FIRST,
    EXIT_IMMEDIATELY,
    CONFIRM_EXIT,
    CUSTOM
}

data class SplashConfig(
    val showDurationMs: Long = 1000,
    val backgroundColor: String = "#0F172A"
)

data class PrivacyDataCollectionConfig(
    val personalData: Boolean = false,
    val accountData: Boolean = false,
    val email: Boolean = false,
    val location: Boolean = false,
    val camera: Boolean = false,
    val microphone: Boolean = false,
    val deviceIdentifiers: Boolean = false,
    val analytics: Boolean = false,
    val advertising: Boolean = false,
    val crashReports: Boolean = false,
    val gameplayData: Boolean = false
)

data class PrivacyConfig(
    val contactEmail: String = "volkanolgac@gmail.com",
    val developerName: String = "Volkan Olgaç",
    val dataCollection: PrivacyDataCollectionConfig = PrivacyDataCollectionConfig(),
    val dataUse: List<String> = emptyList(),
    val thirdParties: List<String> = emptyList(),
    val retention: String = "",
    val deletion: String = ""
)

data class AppConfig(
    val appName: String = "Volkan Web2Android",
    val applicationId: String = "com.aistudio.volkanweb2android.host",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val webAppEntry: String = "index.html",
    val webAppOrigin: String = "https://appassets.androidplatform.net",
    val webAppAssetDirectory: String = "web",
    val orientationMode: OrientationMode = OrientationMode.SENSOR,
    val fullscreenMode: FullscreenMode = FullscreenMode.EDGE_TO_EDGE,
    val keepScreenOn: Boolean = false,
    val externalLinkPolicy: ExternalLinkPolicy = ExternalLinkPolicy.OPEN_IN_EXTERNAL_BROWSER,
    val downloadPolicyEnabled: Boolean = true,
    val uploadPolicyEnabled: Boolean = true,
    val multipleWindowPolicy: MultipleWindowPolicy = MultipleWindowPolicy.BLOCK,
    val cameraEnabled: Boolean = false,
    val microphoneEnabled: Boolean = false,
    val geolocationEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val shareEnabled: Boolean = true,
    val clipboardEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val filePickerEnabled: Boolean = true,
    val mediaCaptureEnabled: Boolean = false,
    val allowRemoteIframes: Boolean = false,
    val allowExternalNavigation: Boolean = false,
    val allowedRemoteOrigins: List<String> = emptyList(),
    val allowedExternalSchemes: List<String> = listOf("mailto:", "tel:", "sms:", "geo:", "market:", "intent:", "volkan:"),
    val backButtonPolicy: BackButtonPolicy = BackButtonPolicy.WEB_HISTORY_FIRST,
    val offlineMode: Boolean = true,
    val networkRequired: Boolean = false,
    val debugToolsEnabled: Boolean = true,
    val serviceWorkerEnabled: Boolean = false,
    val customUserAgentSuffix: String = "VolkanWeb2Android/1.0",
    val statusBarMode: String = "DARK",
    val navigationBarMode: String = "DARK",
    val splashConfig: SplashConfig = SplashConfig(),
    val privacyConfig: PrivacyConfig = PrivacyConfig()
) {
    companion object {
        fun loadFromAssets(context: Context, filename: String = "app-config.json"): AppConfig {
            return try {
                val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
                parse(jsonString)
            } catch (e: Exception) {
                VolkanLogger.w(VolkanLogger.TAG_SHELL, "Could not load $filename, falling back to default config: ${e.message}")
                AppConfig()
            }
        }

        fun parse(jsonString: String): AppConfig {
            val json = JSONObject(jsonString)
            val splashObj = json.optJSONObject("splashConfiguration")
            val splash = SplashConfig(
                showDurationMs = splashObj?.optLong("showDurationMs", 1000L) ?: 1000L,
                backgroundColor = splashObj?.optString("backgroundColor", "#0F172A") ?: "#0F172A"
            )

            val privacyObj = json.optJSONObject("privacy")
            val dcObj = privacyObj?.optJSONObject("dataCollection")
            val privacy = if (privacyObj != null) {
                val dataUseList = mutableListOf<String>()
                privacyObj.optJSONArray("dataUse")?.let { arr ->
                    for (i in 0 until arr.length()) dataUseList.add(arr.optString(i))
                }
                val thirdPartyList = mutableListOf<String>()
                privacyObj.optJSONArray("thirdParties")?.let { arr ->
                    for (i in 0 until arr.length()) thirdPartyList.add(arr.optString(i))
                }
                PrivacyConfig(
                    contactEmail = privacyObj.optString("contactEmail", "volkanolgac@gmail.com"),
                    developerName = privacyObj.optString("developerName", "Volkan Olgaç"),
                    dataCollection = PrivacyDataCollectionConfig(
                        personalData = dcObj?.optBoolean("personalData", false) ?: false,
                        accountData = dcObj?.optBoolean("accountData", false) ?: false,
                        email = dcObj?.optBoolean("email", false) ?: false,
                        location = dcObj?.optBoolean("location", false) ?: false,
                        camera = dcObj?.optBoolean("camera", false) ?: false,
                        microphone = dcObj?.optBoolean("microphone", false) ?: false,
                        deviceIdentifiers = dcObj?.optBoolean("deviceIdentifiers", false) ?: false,
                        analytics = dcObj?.optBoolean("analytics", false) ?: false,
                        advertising = dcObj?.optBoolean("advertising", false) ?: false,
                        crashReports = dcObj?.optBoolean("crashReports", false) ?: false,
                        gameplayData = dcObj?.optBoolean("gameplayData", false) ?: false
                    ),
                    dataUse = dataUseList,
                    thirdParties = thirdPartyList,
                    retention = privacyObj.optString("retention", ""),
                    deletion = privacyObj.optString("deletion", "")
                )
            } else {
                PrivacyConfig()
            }

            val remoteOrigins = mutableListOf<String>()
            json.optJSONArray("allowedRemoteOrigins")?.let { arr ->
                for (i in 0 until arr.length()) {
                    remoteOrigins.add(arr.optString(i))
                }
            }

            val externalSchemes = mutableListOf<String>()
            json.optJSONArray("allowedExternalSchemes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    externalSchemes.add(arr.optString(i))
                }
            }
            if (externalSchemes.isEmpty()) {
                externalSchemes.addAll(listOf("mailto:", "tel:", "sms:", "geo:", "market:", "intent:", "volkan:"))
            }

            return AppConfig(
                appName = json.optString("appName", "Volkan Web2Android"),
                applicationId = json.optString("applicationId", "com.aistudio.volkanweb2android.host"),
                versionName = json.optString("versionName", "1.0.0"),
                versionCode = json.optInt("versionCode", 1),
                webAppEntry = json.optString("webAppEntry", "index.html"),
                webAppOrigin = json.optString("webAppOrigin", "https://appassets.androidplatform.net"),
                webAppAssetDirectory = json.optString("webAppAssetDirectory", "web"),
                orientationMode = try {
                    OrientationMode.valueOf(json.optString("orientationMode", "SENSOR").uppercase())
                } catch (_: Exception) { OrientationMode.SENSOR },
                fullscreenMode = try {
                    FullscreenMode.valueOf(json.optString("fullscreenMode", "EDGE_TO_EDGE").uppercase())
                } catch (_: Exception) { FullscreenMode.EDGE_TO_EDGE },
                keepScreenOn = json.optBoolean("keepScreenOn", false),
                externalLinkPolicy = try {
                    ExternalLinkPolicy.valueOf(json.optString("externalLinkPolicy", "OPEN_IN_EXTERNAL_BROWSER").uppercase())
                } catch (_: Exception) { ExternalLinkPolicy.OPEN_IN_EXTERNAL_BROWSER },
                downloadPolicyEnabled = json.optString("downloadPolicy", "ENABLED").equals("ENABLED", ignoreCase = true),
                uploadPolicyEnabled = json.optString("uploadPolicy", "ENABLED").equals("ENABLED", ignoreCase = true),
                multipleWindowPolicy = try {
                    MultipleWindowPolicy.valueOf(json.optString("multipleWindowPolicy", "BLOCK").uppercase())
                } catch (_: Exception) { MultipleWindowPolicy.BLOCK },
                cameraEnabled = json.optBoolean("cameraEnabled", false),
                microphoneEnabled = json.optBoolean("microphoneEnabled", false),
                geolocationEnabled = json.optBoolean("geolocationEnabled", false),
                notificationsEnabled = json.optBoolean("notificationsEnabled", false),
                vibrationEnabled = json.optBoolean("vibrationEnabled", true),
                shareEnabled = json.optBoolean("shareEnabled", true),
                clipboardEnabled = json.optBoolean("clipboardEnabled", true),
                biometricEnabled = json.optBoolean("biometricEnabled", false),
                filePickerEnabled = json.optBoolean("filePickerEnabled", true),
                mediaCaptureEnabled = json.optBoolean("mediaCaptureEnabled", false),
                allowRemoteIframes = json.optBoolean("allowRemoteIframes", false),
                allowExternalNavigation = json.optBoolean("allowExternalNavigation", false),
                allowedRemoteOrigins = remoteOrigins,
                allowedExternalSchemes = externalSchemes,
                backButtonPolicy = try {
                    BackButtonPolicy.valueOf(json.optString("backButtonPolicy", "WEB_HISTORY_FIRST").uppercase())
                } catch (_: Exception) { BackButtonPolicy.WEB_HISTORY_FIRST },
                offlineMode = json.optBoolean("offlineMode", true),
                networkRequired = json.optBoolean("networkRequired", false),
                debugToolsEnabled = json.optBoolean("debugToolsEnabled", true),
                serviceWorkerEnabled = json.optBoolean("serviceWorkerEnabled", false),
                customUserAgentSuffix = json.optString("customUserAgentSuffix", "VolkanWeb2Android/1.0"),
                statusBarMode = json.optString("statusBarMode", "DARK"),
                navigationBarMode = json.optString("navigationBarMode", "DARK"),
                splashConfig = splash,
                privacyConfig = privacy
            )
        }
    }
}
