package com.skunkworks.magic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Data class that represents a standardized API response structure.
 *
 * @param T The type of the data contained in the response.
 * @property data The actual data of type T returned by the API. It can be null if there is an error.
 * @property status The status of the API response, typically indicating success or failure.
 * @property errorCode An optional error code provided in case of a failure or error.
 * @property message An optional message providing more details about the status or error.
 */
@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val status: String,
    @SerialName("error_code")
    val errorCode: String? = null,
    val message: String? = null
)