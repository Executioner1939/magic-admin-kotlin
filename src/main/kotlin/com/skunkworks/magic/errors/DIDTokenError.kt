package com.skunkworks.magic.errors

/**
 * Represents different types of errors that can occur when working with DID tokens.
 *
 * This sealed class extends the [Exception] class to provide detailed error messages and handling mechanisms
 * for various scenarios such as invalid public address, signature mismatches, token validity issues, audience mismatches,
 * base64 decoding errors, and token format errors.
 *
 * @constructor Creates a new [DIDTokenError] with the specified message.
 * @constructor Creates a new [DIDTokenError] with the specified message and cause.
 */
sealed class DIDTokenError : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    /**
     * Represents an error that indicates the format of a public address is invalid.
     *
     * This object extends the [DIDTokenError] class and provides a specific error message
     * to identify invalid public address formats.
     */
    data object InvalidPublicAddress : DIDTokenError("Invalid public address format.") {
        private fun readResolve(): Any = InvalidPublicAddress
    }

    /**
     * Object representing an error case where there is a mismatch between the signature in the 'proof' field and the expected signature in the 'claim' field.
     *
     * This error is thrown when validating a DID token and the provided signature does not match the expected signature derived from the token's claim.
     * In such cases, a new token should be generated with an intended issuer to resolve the mismatch.
     *
     * @constructor Creates a new instance of SignatureMismatch error with a predefined error message indicating the signature mismatch.
     */
    data object SignatureMismatch : DIDTokenError(
        "Signature mismatch between 'proof' and 'claim'. Please generate a new token with an intended issuer."
    ) {
        private fun readResolve(): Any = SignatureMismatch
    }

    /**
     * Indicates that a DID token has expired.
     *
     * This error is thrown when a token's expiration timestamp is less than the current time,
     * suggesting the token is no longer valid and a new token needs to be generated.
     *
     * @constructor Initializes the TokenExpired object with a predefined error message.
     */
    data object TokenExpired : DIDTokenError(
        "Given DID token has expired. Please generate a new one."
    ) {
        private fun readResolve(): Any = TokenExpired
    }

    /**
     * Indicates that the given Decentralized Identifier (DID) token is not yet valid.
     *
     * This exception is thrown when a token's 'nbf' (not before) claim indicates a future time.
     * It suggests that the token cannot be used at the current time and might need to be
     * regenerated with an appropriate 'nbf' value.
     */
    data object TokenNotYetValid : DIDTokenError(
        "Given DID token cannot be used at this time. Please check the 'nbf' field and regenerate a token with a suitable value."
    ) {
        private fun readResolve(): Any = TokenNotYetValid
    }

    /**
     * Represents an error indicating that the audience of the DID token does not match the expected client ID.
     *
     * This error is used to signal that the secret key does not match the application which generated the DID token.
     */
    data object AudienceMismatch : DIDTokenError(
        "Audience does not match client ID. Please ensure your secret key matches the application which generated the DID token."
    ) {
        private fun readResolve(): Any = AudienceMismatch
    }

    /**
     * Represents an error that occurs during the base64 decoding process of a DID token.
     *
     * This exception is thrown when there is a failure to decode a base64 encoded token string,
     * usually indicating that the token string is not properly formatted or corrupted.
     *
     * @constructor Creates a new instance of [Base64DecodingError] with the specified message and cause.
     *
     * @param message A detailed message that provides more context about the base64 decoding error.
     * @param cause The underlying cause of the error, which can be another exception that triggered this error.
     */
    class Base64DecodingError(message: String, cause: Throwable) : DIDTokenError(message, cause)

    /**
     * Represents an error that occurs when the token format is invalid or cannot be properly decoded.
     *
     * This exception is thrown when there is a failure to decode the token structure after base64 decoding,
     * usually indicating that the token's internal format is incorrect or corrupted.
     *
     * @constructor Creates a new instance of [InvalidTokenFormat] with the specified message and optional cause.
     *
     * @param message A detailed message that provides more context about the token format error.
     * @param cause The underlying cause of the error, which can be another exception that triggered this error.
     */
    class InvalidTokenFormat(message: String, cause: Throwable? = null) : DIDTokenError(message, cause ?: Exception(message))
}