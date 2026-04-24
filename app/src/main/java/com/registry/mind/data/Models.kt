package com.registry.mind.data

import com.google.gson.annotations.SerializedName

data class RegistryPacket(
    @SerializedName("header")
    val header: Header,
    @SerializedName("payload")
    val payload: Payload,
    @SerializedName("navigation_meta")
    val navigationMeta: NavigationMeta
)

data class Header(
    @SerializedName("protocol")
    val protocol: String = "registry-mind-v1",
    @SerializedName("device")
    val device: String = "Oppo_Find_X9_Native",
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("auth_token")
    val authToken: String
)

data class Payload(
    @SerializedName("image_data")
    val imageData: String,
    @SerializedName("ocr_content")
    val ocrContent: String,
    @SerializedName("source_app")
    val sourceApp: String,
    @SerializedName("ai_guess")
    val aiGuess: String? = null
)

data class NavigationMeta(
    @SerializedName("role")
    val role: String = "registry_sensor",
    @SerializedName("session_state")
    val sessionState: String = "inactive",
    @SerializedName("tag")
    val tag: String? = null
)
