package com.skunkworks.magic.errors

import kotlinx.serialization.Serializable

/**
 * Represents the response for an error.
 *
 * This data class is used to encapsulate details about an error that has occurred,
 * providing a status, an optional message, and an optional error code.
 *
 * @property status The status of the error.
 * @property message An optional message providing more details about the error.
 * @property errorCode An optional code representing the specific error that occurred.
 */
@Serializable
data class ErrorResponse(
    val status: String,
    val message: String? = null,
    val errorCode: String? = null
)