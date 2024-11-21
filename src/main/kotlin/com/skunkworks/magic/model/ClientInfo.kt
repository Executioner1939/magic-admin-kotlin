package com.skunkworks.magic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing the information of a client.
 *
 * @property clientId The unique identifier of the client.
 * @property appScope The scope of the application's access permissions.
 */
@Serializable
data class ClientInfo(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("app_scope")
    val appScope: String
)