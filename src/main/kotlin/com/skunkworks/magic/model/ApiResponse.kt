package com.skunkworks.magic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 *
 */
@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val status: String,
    @SerialName("error_code")
    val errorCode: String? = null,
    val message: String? = null
)