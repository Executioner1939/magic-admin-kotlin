package com.skunkworks.magic.errors

import com.skunkworks.magic.model.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents various exceptions that can occur in the MagicAdmin context.
 *
 * This sealed class extends the `Exception` class to provide customized error handling
 * for specific scenarios such as rate limiting, bad requests, unauthorized access,
 * forbidden actions, and general API errors.
 */
@Serializable
sealed class MagicAdminException : Exception() {
    abstract override val message: String
    abstract val statusCode: Int

    @Serializable
    @SerialName("rateLimitExceeded")
    data class RateLimitExceeded(
        override val message: String,
        override val statusCode: Int = 429
    ) : MagicAdminException()

    @Serializable
    @SerialName("badRequest")
    data class BadRequest(
        override val message: String,
        override val statusCode: Int = 400
    ) : MagicAdminException()

    @Serializable
    @SerialName("unauthorized")
    data class Unauthorized(
        override val message: String,
        override val statusCode: Int = 401
    ) : MagicAdminException()

    @Serializable
    @SerialName("forbidden")
    data class Forbidden(
        override val message: String,
        override val statusCode: Int = 403
    ) : MagicAdminException()

    @Serializable
    @SerialName("apiError")
    data class ApiError(
        override val message: String,
        override val statusCode: Int
    ) : MagicAdminException()

    companion object {

        /**
         * Handles an API response and throws an appropriate exception based on the error code.
         *
         * @param response The `ApiResponse` object containing the status, error code, and message from the API.
         * @return This function does not return anything as it always throws an exception.
         * @throws Unauthorized If the error code is "err_code_unauthorized".
         * @throws BadRequest If the error code is "err_code_bad_request".
         * @throws Forbidden If the error code is "err_code_forbidden".
         * @throws RateLimitExceeded If the error code is "err_code_rate_limited".
         * @throws ApiError If the error code does not match any of the predefined error codes or if the API response is invalid.
         */
        fun fromApiResponse(response: ApiResponse<*>): Nothing {
            if (response.status == "fail") {
                val error = when (response.errorCode) {
                    "err_code_unauthorized" -> Unauthorized(
                        message = response.message ?: "Unauthorized request"
                    )
                    "err_code_bad_request" -> BadRequest(
                        message = response.message ?: "Bad request"
                    )
                    "err_code_forbidden" -> Forbidden(
                        message = response.message ?: "Forbidden request"
                    )
                    "err_code_rate_limited" -> RateLimitExceeded(
                        message = response.message ?: "Rate limit exceeded"
                    )
                    else -> ApiError(
                        message = response.message ?: "API Error",
                        statusCode = 500
                    )
                }
                throw error
            }
            throw ApiError(
                message = "Invalid API response",
                statusCode = 500
            )
        }
    }
}