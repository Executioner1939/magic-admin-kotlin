package com.skunkworks.magic.api

import com.skunkworks.magic.errors.MagicAdminException
import com.skunkworks.magic.model.ApiResponse
import com.skunkworks.magic.model.ClientInfo
import com.skunkworks.magic.model.UserInfo
import com.skunkworks.magic.token.Token
import com.skunkworks.magic.wallet.WalletType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Client for interacting with the Magic Admin API.
 *
 * @property secretKey The secret key for authenticating API requests.
 * @property magicClientId The client ID for the Magic application.
 * @property httpClient Optional custom HTTP client.
 */
class MagicAdminClient(
    private val secretKey: String,
    baseUrl: String = "https://api.magic.link",
    httpClient: HttpClient? = null
) : AutoCloseable {
    companion object {
        const val API_SECRET_HEADER = "X-Magic-Secret-Key"
    }

    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(DefaultRequest) {
            header(API_SECRET_HEADER, secretKey)
            contentType(ContentType.Application.Json)
            url(baseUrl)
        }
    }

    private val endpoints = MagicAdminEndpointsImpl(client)

    private val magicClientId: String

    init {
        // Fetch the client ID when creating the instance
        magicClientId = runBlocking {
            fetchClientInfo().clientId ?: throw IllegalStateException("Client ID is missing in the response")
        }
    }

    private suspend fun fetchClientInfo(): ClientInfo {
        return endpoints.getClientInfo().data
            ?: throw IllegalStateException("Failed to fetch client info from Magic API")
    }

    suspend fun getUserMetadataByToken(
        didToken: String,
        walletType: WalletType = WalletType.NONE
    ): Result<UserInfo> = runCatching {
        val token = Token.decode(didToken)
        token.validate(magicClientId)
        getUserMetadataByIssuer(token.issuer, walletType).getOrThrow()
    }

    suspend fun getUserMetadataByIssuer(
        issuer: String,
        walletType: WalletType = WalletType.NONE
    ): Result<UserInfo> = runCatching {
        val response = endpoints.getUserInfo(
            issuer,
            walletType.takeUnless { it == WalletType.NONE }?.toString()
        )

        response.data ?: MagicAdminException.fromApiResponse(response)
    }

    suspend fun logoutByIssuer(issuer: String): Result<Unit> = runCatching {
        val response = endpoints.logoutUser(issuer)
        if (response.status == "fail") {
            MagicAdminException.fromApiResponse(response)
        }
    }

    suspend fun logoutByToken(didToken: String): Result<Unit> = runCatching {
        val token = Token.decode(didToken)
        token.validate(magicClientId)
        logoutByIssuer(token.issuer).getOrThrow()
    }

    override fun close() {
        client.close()
    }
}

internal class MagicAdminEndpointsImpl(
    private val client: HttpClient
) : MagicAdminEndpoints {
    override suspend fun getUserInfo(
        issuer: String,
        walletType: String?
    ): ApiResponse<UserInfo> = client.get("/v1/admin/auth/user/get") {
        parameter("issuer", issuer)
        walletType?.let { parameter("wallet_type", it) }
    }.body()

    override suspend fun logoutUser(issuer: String): ApiResponse<Unit> {
        return client.post("/v2/admin/auth/user/logout") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("issuer" to issuer))
        }.body()
    }

    override suspend fun getClientInfo(): ApiResponse<ClientInfo> =
        client.get("/v1/admin/client/get").body()
}

/**
 * Interface defining endpoints for Magic Admin functionalities.
 */
interface MagicAdminEndpoints {
    suspend fun getUserInfo(issuer: String, walletType: String? = null): ApiResponse<UserInfo>
    suspend fun logoutUser(issuer: String): ApiResponse<Unit>
    suspend fun getClientInfo(): ApiResponse<ClientInfo>
}