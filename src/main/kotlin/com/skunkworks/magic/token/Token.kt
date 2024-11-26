package com.skunkworks.magic.token

import com.skunkworks.magic.errors.DIDTokenError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets
import java.security.SignatureException
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The `Token` class represents a token containing a proof and associated claims.
 * It provides functionalities to decode, validate, and extract specific information
 * like the public address from the token.
 *
 * @property proof The proof component of the token.
 * @property claim The `Claim` associated with the token.
 */
class Token(
    val proof: String,
    val claim: Claim
) {
    val issuer: String get() = claim.iss

    companion object {
        private const val NBF_GRACE_PERIOD = 300

        /**
         * Decodes a provided base64-encoded token string and parses it into a `Token` object.
         *
         * This method first decodes the token from base64 format, and then parses it into
         * two parts. The first part represents the proof, and the second part is the claim in JSON format.
         * If any decoding or parsing errors occur, appropriate `DIDTokenError` exceptions are thrown.
         *
         * @param token The base64-encoded token string to be decoded.
         * @return A `Token` object containing the decoded proof and claim.
         * @throws DIDTokenError.Base64DecodingError If the base64-decoding process fails.
         * @throws DIDTokenError.InvalidTokenFormat If the token format is invalid or cannot be parsed correctly.
         */
        @OptIn(ExperimentalEncodingApi::class)
        fun decode(token: String): Token {
            val decoded = try {
                Base64.decode(token)
            } catch (e: Exception) {
                throw DIDTokenError.Base64DecodingError("Failed to decode base64 token", e)
            }

            try {
                val pieces = Json.decodeFromString<List<String>>(
                    String(decoded, StandardCharsets.UTF_8)
                )
                if (pieces.size != 2) {
                    throw DIDTokenError.InvalidTokenFormat("Token must contain exactly two parts")
                }
                val claim = Json.decodeFromString<Claim>(pieces[1])
                return Token(pieces[0], claim)
            } catch (e: Exception) {
                throw DIDTokenError.InvalidTokenFormat("Failed to decode token format", e)
            }
        }
    }

    /**
     * Extracts and validates the public address from the issuer field of the token.
     *
     * This method assumes that the issuer is a properly formatted string, containing the address
     * as the third component separated by colons. It checks whether the third component is a valid
     * Ethereum address and returns the address in lowercase if it is valid.
     *
     * @return The validated and lowercased Ethereum public address.
     * @throws DIDTokenError.InvalidPublicAddress if the address is not in the correct format.
     */
    fun getPublicAddress(): String {
        val parts = issuer.split(":")
        if (parts.size != 3 || !parts[2].matches(Regex("^0x[a-fA-F0-9]{40}$"))) {
            throw DIDTokenError.InvalidPublicAddress
        }
        return parts[2].lowercase()
    }

    /**
     * Validates the DID token against the provided client ID.
     *
     * This method checks the token's expiration time, not-before time, audience, and signature.
     * If any of these validations fail, appropriate exceptions are thrown.
     *
     * @param clientId The ID of the client for which the token should be valid.
     * @throws DIDTokenError.TokenExpired if the token has expired.
     * @throws DIDTokenError.TokenNotYetValid if the token is not yet valid.
     * @throws DIDTokenError.AudienceMismatch if the audience of the token does not match the provided client ID.
     * @throws DIDTokenError.SignatureMismatch if the token's signature is invalid.
     */
    fun validate(clientId: String) {
        val now = Instant.now().epochSecond

        if (now > claim.ext) {
            throw DIDTokenError.TokenExpired
        }

        if (now < claim.nbf - NBF_GRACE_PERIOD) {
            throw DIDTokenError.TokenNotYetValid
        }

        if (claim.aud != clientId) {
            throw DIDTokenError.AudienceMismatch
        }

        val messageBytes = Json.encodeToString(claim).toByteArray(StandardCharsets.UTF_8)
        val messageHash = Sign.getEthereumMessageHash(messageBytes)

        val signatureBytes = Numeric.hexStringToByteArray(proof)
        if (signatureBytes.size != 65) {
            throw DIDTokenError.SignatureMismatch
        }

        val r = signatureBytes.copyOfRange(0, 32)
        val s = signatureBytes.copyOfRange(32, 64)
        var v = signatureBytes[64].toInt() and 0xFF // Ensure unsigned byte

        // Adjust v value if necessary
        if (v < 27) {
            v += 27
        }

        if (v != 27 && v != 28) {
            throw DIDTokenError.SignatureMismatch
        }

        val signatureData = Sign.SignatureData(v.toByte(), r, s)

        try {
            // Recover the public key using web3j's method
            val recoveredKey = Sign.signedMessageHashToKey(messageHash, signatureData)
            val recoveredAddress = Keys.getAddress(recoveredKey)
            val tokenAddress = getPublicAddress().removePrefix("0x").lowercase()

            if (recoveredAddress.lowercase() != tokenAddress) {
                throw DIDTokenError.SignatureMismatch
            }
        } catch (e: SignatureException) {
            throw DIDTokenError.SignatureMismatch
        }
    }
}