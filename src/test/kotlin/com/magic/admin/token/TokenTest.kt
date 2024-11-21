package com.magic.admin.token

import com.skunkworks.magic.errors.DIDTokenError
import com.skunkworks.magic.token.Claim
import com.skunkworks.magic.token.Token
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class TokenTest {
    private val testDIDToken = "WyIweGFhNTBiZTcwNzI5Y2E3MDViYTdjOGQwMDE4NWM2ZjJkYTQ3OWQwZm" +
                               "NkZTUzMTFjYTRjZTViMWJhNzE1YzhhNzIxYzVmMTk0ODQzNGY5NmZmNTc3ZDdiMmI2YWQ4MmQ" +
                               "zZGQ1YTI0NTdmZTY5OThiMTM3ZWQ5YmMwOGQzNmU1NDljMWIiLCJ7XCJpYXRcIjoxNTg2NzY0" +
                               "MjcwLFwiZXh0XCI6MTExNzM1Mjg1MDAsXCJpc3NcIjpcImRpZDpldGhyOjB4NEI3M0M1ODM3M" +
                               "EFFZmNFZjg2QTYwMjFhZkNEZTU2NzM1MTEzNzZCMlwiLFwic3ViXCI6XCJOanJBNTNTY1E4SV" +
                               "Y4ME5Kbng0dDNTaGk5LWtGZkY1cWF2RDJWcjBkMWRjPVwiLFwiYXVkXCI6XCJkaWQ6bWFnaWM" +
                               "6NzMxODQ4Y2MtMDg0ZS00MWZmLWJiZGYtN2YxMDM4MTdlYTZiXCIsXCJuYmZcIjoxNTg2NzY0" +
                               "MjcwLFwidGlkXCI6XCJlYmNjODgwYS1mZmM5LTQzNzUtODRhZS0xNTRjY2Q1Yzc0NmRcIixcI" +
                               "mFkZFwiOlwiMHg4NGQ2ODM5MjY4YTFhZjkxMTFmZGVjY2QzOTZmMzAzODA1ZGNhMmJjMDM0NT" +
                               "BiN2ViMTE2ZTJmNWZjOGM1YTcyMmQxZmI5YWYyMzNhYTczYzVjMTcwODM5Y2U1YWQ4MTQxYjl" +
                               "iNDY0MzM4MDk4MmRhNGJmYmIwYjExMjg0OTg4ZjFiXCJ9Il0="

    @Test
    fun `verify token decode and validation`() {
        val expectedIssuer = "did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2"
        val expectedProof = "0xaa50be70729ca705ba7c8d00185c6f2da479d0fcde5311ca4ce5b1ba715c8a721c5f1948434f96ff577d7b2b6ad82d3dd5a2457fe6998b137ed9bc08d36e549c1b"
        val expectedClaim = Claim(
            iat = 1586764270,
            ext = 11173528500,
            nbf = 1586764270,
            iss = "did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2",
            sub = "NjrA53ScQ8IV80NJnx4t3Shi9-kFfF5qavD2Vr0d1dc=",
            aud = "did:magic:731848cc-084e-41ff-bbdf-7f103817ea6b",
            tid = "ebcc880a-ffc9-4375-84ae-154ccd5c746d",
            add = "0x84d6839268a1af9111fdeccd396f303805dca2bc03450b7eb116e2f5fc8c5a722d1fb9af233aa73c5c170839ce5ad8141b9b4643380982da4bfbb0b11284988f1b"
        )

        val token = Token.decode(testDIDToken)
        assertEquals(expectedClaim, token.claim)
        assertEquals(expectedProof, token.proof)
        assertEquals(expectedIssuer, token.issuer)
        assertDoesNotThrow {
            token.validate("did:magic:731848cc-084e-41ff-bbdf-7f103817ea6b")
        }
    }

    @Test
    fun `test decode invalid base64`() {
        assertThrows<DIDTokenError.Base64DecodingError> {
            Token.decode("invalid base64!")
        }
    }

    @Test
    fun `test decode malformed token`() {
        assertThrows<DIDTokenError.InvalidTokenFormat> {
            Token.decode("aW52YWxpZCB0b2tlbiE=") // "invalid token!" in base64
        }
    }

    @Test
    fun `test decode malformed claim`() {
        // A token with valid base64 but invalid claim JSON
        val invalidToken = "WyJwcm9vZiIsIntpbnZhbGlkIGpzb259Il0=" // ["proof","{invalid json}"] in base64
        assertThrows<DIDTokenError.InvalidTokenFormat> {
            Token.decode(invalidToken)
        }
    }

    @Test
    fun `test audience mismatch`() {
        val token = Token.decode(testDIDToken)
        val wrongClientId = "wrong-client-id"

        assertThrows<DIDTokenError.AudienceMismatch> {
            token.validate(wrongClientId)
        }
    }
}