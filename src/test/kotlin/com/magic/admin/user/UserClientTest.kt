package com.magic.admin.user

import com.skunkworks.magic.api.MagicAdminClient
import com.skunkworks.magic.errors.MagicAdminException
import com.skunkworks.magic.model.UserInfo
import com.skunkworks.magic.model.Wallet
import com.skunkworks.magic.token.Claim
import com.skunkworks.magic.token.Token
import com.skunkworks.magic.wallet.WalletType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserClientTest {
    private val testDIDToken = "WyIweGFhNTBiZTcwNzI5Y2E3MDViYTdjOGQwMDE4NWM2ZjJkYTQ3OWQwZmNkZTUzMTFjYTRjZTViMWJhNzE1YzhhNzIxYzVmMTk0ODQzNGY5NmZmNTc3ZDdiMmI2YWQ4MmQzZGQ1YTI0NTdmZTY5OThiMTM3ZWQ5YmMwOGQzNmU1NDljMWIiLCJ7XCJpYXRcIjoxNTg2NzY0MjcwLFwiZXh0XCI6MTExNzM1Mjg1MDAsXCJpc3NcIjpcImRpZDpldGhyOjB4NEI3M0M1ODM3MEFFZmNFZjg2QTYwMjFhZkNEZTU2NzM1MTEzNzZCMlwiLFwic3ViXCI6XCJOanJBNTNTY1E4SVY4ME5Kbng0dDNTaGk5LWtGZkY1cWF2RDJWcjBkMWRjPVwiLFwiYXVkXCI6XCJkaWQ6bWFnaWM6NzMxODQ4Y2MtMDg0ZS00MWZmLWJiZGYtN2YxMDM4MTdlYTZiXCIsXCJuYmZcIjoxNTg2NzY0MjcwLFwidGlkXCI6XCJlYmNjODgwYS1mZmM5LTQzNzUtODRhZS0xNTRjY2Q1Yzc0NmRcIixcImFkZFwiOlwiMHg4NGQ2ODM5MjY4YTFhZjkxMTFmZGVjY2QzOTZmMzAzODA1ZGNhMmJjMDM0NTBiN2ViMTE2ZTJmNWZjOGM1YTcyMmQxZmI5YWYyMzNhYTczYzVjMTcwODM5Y2U1YWQ4MTQxYjliNDY0MzM4MDk4MmRhNGJmYmIwYjExMjg0OTg4ZjFiXCJ9Il0="
    private val testSecret = "sk_test_E123E4567E8901D2"
    private val testClientId = "did:magic:731848cc-084e-41ff-bbdf-7f103817ea6b"
    private val testIssuer = "did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2"

    @BeforeTest
    fun setup() {
        val currentTime = Instant.now().epochSecond
        val mockClaim = Claim(
            iat = currentTime,
            ext = currentTime + 3600,
            iss = testIssuer,
            sub = "NjrA53ScQ8IV80NJnx4t3Shi9-kFfF5qavD2Vr0d1dc=",
            aud = testClientId,
            nbf = currentTime,
            tid = "ebcc880a-ffc9-4375-84ae-154ccd5c746d"
        )

        val mockToken = mockk<Token> {
            every { issuer } returns testIssuer
            every { validate(any<String>()) } just Runs
            every { getPublicAddress() } returns "0x4B73C58370AEfcEf86A6021afCDe5673511376B2"
        }

        mockkObject(Token.Companion)
        every { Token.decode(any<String>()) } returns mockToken
    }

    private fun createMockHttpClient(mockEngine: MockEngine, secret: String): HttpClient {
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }
            install(DefaultRequest) {
                header(MagicAdminClient.API_SECRET_HEADER, secret)
                contentType(ContentType.Application.Json)
            }
        }
    }

    private fun createTestClient(secret: String = testSecret): MagicAdminClient {
        val mockEngine = MockEngine { request ->
            // Log request details for debugging
            println("URL: ${request.url}")
            println("Headers: ${request.headers.entries()}")

            // Always check auth first
            val authHeader = request.headers[MagicAdminClient.API_SECRET_HEADER]
            if (authHeader != testSecret) {
                return@MockEngine respond(
                    content = Json.encodeToString(
                        buildJsonObject {
                            put("status", "fail")
                            put("error_code", "err_code_unauthorized")
                            put("message", "unauthorized")
                        }
                    ),
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            // If auth passes, handle the endpoints
            when (request.url.encodedPath) {
                "/v1/admin/auth/user/get" -> {
                    respond(
                        content = Json.encodeToString(
                            buildJsonObject {
                                put("status", "ok")
                                put("data", Json.encodeToJsonElement(testSuccessData))
                            }
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/v2/admin/auth/user/logout" -> {
                    respond(
                        content = Json.encodeToString(
                            buildJsonObject {
                                put("status", "ok")
                            }
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }

        return MagicAdminClient(
            secretKey = secret,
            magicClientId = testClientId,
            baseUrl = "",
            httpClient = createMockHttpClient(mockEngine, secret)
        )
    }

    private fun createFailureTestClient(): MagicAdminClient {
        val mockEngine = MockEngine { request ->
            println("FAILURE URL: ${request.url}")
            println("FAILURE Headers: ${request.headers.entries()}")

            val failureResponse = buildJsonObject {
                put("status", "fail")
                put("error_code", "err_code_internal_server_error")
                put("message", "internal server error")
            }

            respond(
                content = Json.encodeToString(failureResponse),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        return MagicAdminClient(
            secretKey = testSecret,
            magicClientId = testClientId,
            baseUrl = "",
            httpClient = createMockHttpClient(mockEngine, testSecret)
        )
    }

    @Test
    fun `get metadata by token returns user info`() = runTest {
        val client = createTestClient()
        val result = client.getUserMetadataByToken(testDIDToken).getOrThrow()

        assertEquals("user@email.com", result.email)
        assertEquals("did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2", result.issuer)
        assertEquals("0x4B73C58370AEfcEf86A6021afCDe5673511376B2", result.publicAddress)
    }

    @Test
    fun `get metadata with wallet type returns wallet info`() = runTest {
        val client = createTestClient()
        val result = client.getUserMetadataByToken(testDIDToken, WalletType.SOLANA).getOrThrow()

        assertEquals("user@email.com", result.email)
        assertEquals("did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2", result.issuer)
        assertEquals("0x4B73C58370AEfcEf86A6021afCDe5673511376B2", result.publicAddress)
        assertNotNull(result.wallets)
        assertEquals(1, result.wallets?.size)
        assertEquals(WalletType.SOLANA.toString(), result.wallets?.first()?.walletType)
    }

    @Test
    fun `get metadata with ANY wallet type returns all wallets`() = runTest {
        val client = createTestClient()
        val result = client.getUserMetadataByToken(testDIDToken, WalletType.ANY).getOrThrow()

        assertEquals("user@email.com", result.email)
        assertNotNull(result.wallets)
        assertEquals(1, result.wallets?.size)
        assertEquals(WalletType.SOLANA.toString(), result.wallets?.first()?.walletType)
    }

    @Test
    fun `wrong secret key throws unauthorized error`() = runTest {
        val client = createTestClient("wrong_secret")

        val result = client.getUserMetadataByToken(testDIDToken)
        assertFailsWith<MagicAdminException.Unauthorized> {
            result.getOrThrow()
        }
    }

    @Test
    fun `backend failure returns APIError`() = runTest {
        val client = createFailureTestClient()

        val result = client.getUserMetadataByToken(testDIDToken)
        assertFailsWith<MagicAdminException.ApiError> {
            result.getOrThrow()
        }
    }

    @Test
    fun `logout by token succeeds`() = runTest {
        val client = createTestClient()
        val result = client.logoutByToken(testDIDToken)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `logout with backend failure returns APIError`() = runTest {
        val client = createFailureTestClient()

        val result = client.logoutByToken(testDIDToken)
        assertFailsWith<MagicAdminException.ApiError> {
            result.getOrThrow()
        }
    }

    private companion object {
        val testSuccessData = UserInfo(
            email = "user@email.com",
            issuer = "did:ethr:0x4B73C58370AEfcEf86A6021afCDe5673511376B2",
            publicAddress = "0x4B73C58370AEfcEf86A6021afCDe5673511376B2",
            wallets = listOf(
                Wallet(
                    network = "MAINNET",
                    publicAddress = "foobar",
                    walletType = "SOLANA"
                )
            )
        )
    }
}