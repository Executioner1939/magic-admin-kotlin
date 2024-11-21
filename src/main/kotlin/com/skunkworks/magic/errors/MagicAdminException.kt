package com.skunkworks.magic.errors

import com.skunkworks.magic.model.ApiResponse
import io.ktor.client.plugins.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A sealed class representing various exceptions that can occur in the Magic Admin environment.
 *
 * This class is designed to handle different types of errors that might be encountered
 * when interacting with Magic Admin APIs, providing appropriate status codes and messages
 * for each error type. It extends the [Exception] class.
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
        fun from(response: ClientRequestException): MagicAdminException {
            return when (response.response.status.value) {
                429 -> RateLimitExceeded(response.message)
                400 -> BadRequest(response.message)
                401 -> Unauthorized(response.message)
                403 -> Forbidden(response.message)
                else -> ApiError(response.message, response.response.status.value)
            }
        }

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