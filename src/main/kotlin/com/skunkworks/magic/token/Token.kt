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

class Token(
    val proof: String,
    val claim: Claim
) {
    val issuer: String get() = claim.iss

    companion object {
        private const val NBF_GRACE_PERIOD = 300

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
     * Validates a client's token by performing various checks such as expiry, validity, audience match,
     * and signature verification. This method throws appropriate errors if any validation step fails.
     *
     * @param clientId The unique identifier of the client for whom the token is being validated.
     * @throws DIDTokenError.TokenExpired if the token has expired.
     * @throws DID*/
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