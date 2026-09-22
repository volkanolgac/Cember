package com.example.volkan.bridge

import org.json.JSONObject

data class BridgeRequest(
    val requestId: String,
    val action: String,
    val params: JSONObject
)

data class BridgeResponse(
    val requestId: String,
    val success: Boolean,
    val data: JSONObject? = null,
    val error: String? = null,
    val message: String? = null
) {
    fun toJson(): String {
        val json = JSONObject().apply {
            put("requestId", requestId)
            put("success", success)
            if (data != null) put("data", data)
            if (error != null) put("error", error)
            if (message != null) put("message", message)
        }
        return json.toString()
    }
}
