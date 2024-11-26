package com.skunkworks.magic.client

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
import kotlinx.serialization.json.Json

/**
 * A client class for interacting with Magic Admin APIs. This client provides functionalities
 * to manage users, such as fetching user metadata and logging users out.
 *
 * @property secretKey The secret API key used to authenticate requests.
 * @property baseUrl The base URL for the Magic Admin API. Default is "https://api.magic.link".
 * @property httpClient An optional custom HttpClient instance for making HTTP requests.
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

    // Lazy initialization of magicClientId
    private var magicClientId: String? = null

    /**
     * Fetches the client ID if it's not cached; otherwise, retrieves it from the Magic Admin API.
     *
     * @return The client ID as a String.
     */
    private suspend fun getMagicClientId(): String {
        return magicClientId ?: fetchClientInfo().getOrThrow().clientId.also {
            magicClientId = it
        }
    }

    /**
     * Fetches the client information from the Magic Admin API.
     *
     * @return A `Result` object containing `ClientInfo` if the operation is successful, or an exception if it fails.
     */
    private suspend fun fetchClientInfo(): Result<ClientInfo> = runCatching {
        val response = endpoints.getClientInfo()
        response.data ?: MagicAdminException.fromApiResponse(response)
    }

    /**
     * Fetches the user metadata based on the provided DID token.
     *
     * @param didToken The Decentralized ID (DID) token of the user.
     * @param walletType The type of wallet the user is using. Default is `WalletType.NONE`.
     * @return A `Result` object containing `UserInfo` if the operation is successful, or an exception if it fails.
     */
    suspend fun getUserMetadataByToken(
        didToken: String,
        walletType: WalletType = WalletType.NONE
    ): Result<UserInfo> = runCatching {
        val token = Token.decode(didToken)
        token.validate(getMagicClientId())
        getUserMetadataByIssuer(token.issuer, walletType).getOrThrow()
    }

    /**
     * Fetches the user metadata based on the provided issuer.
     *
     * @param issuer The unique identifier (issuer) of the user.
     * @param walletType The type of wallet the user is using. Default is `WalletType.NONE`.
     * @return A `Result` object containing `UserInfo` if the operation is successful, or an exception if it fails.
     */
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

    /**
     * Logs out a user based on their issuer.
     *
     * @param issuer The unique identifier (issuer) of the user.
     * @return A `Result` object containing `Unit` if the operation is successful, or an exception if it fails.
     */
    suspend fun logoutByIssuer(issuer: String): Result<Unit> = runCatching {
        val response = endpoints.logoutUser(issuer)
        if (response.status == "fail") {
            MagicAdminException.fromApiResponse(response)
        }
    }

    /**
     * Logs out a user based on the provided Decentralized ID (DID) token.
     *
     * @param didToken The DID token used to identify and authenticate the user.
     * @return A `Result` object containing `Unit` if the operation is successful, or an exception if it fails.
     */
    suspend fun logoutByToken(didToken: String): Result<Unit> = runCatching {
        val token = Token.decode(didToken)
        token.validate(getMagicClientId())
        logoutByIssuer(token.issuer).getOrThrow()
    }

    override fun close() {
        client.close()
    }
}

/**
 * Implementation class for MagicAdminEndpoints interface.
 *
 * @property client The HTTP client used to make requests to the Magic Admin API endpoints.
 */
internal class MagicAdminEndpointsImpl(
    private val client: HttpClient
) : MagicAdminEndpoints {

    /**
     * Retrieves the user information based on the provided issuer and optional wallet type.
     *
     * @param issuer The unique identifier (issuer) of the user.
     * @param walletType Optional parameter specifying the type of wallet associated with the user.
     * @return An ApiResponse object containing the UserInfo data.
     */
    override suspend fun getUserInfo(
        issuer: String,
        walletType: String?
    ): ApiResponse<UserInfo> = client.get("/v1/admin/auth/user/get") {
        parameter("issuer", issuer)
        walletType?.let { parameter("wallet_type", it) }
    }.body()

    /**
     * Logs out a user based on the provided issuer identifier.
     *
     * @param issuer The unique identifier (issuer) of the user to be logged out.
     * @return An ApiResponse object indicating the outcome of the logout operation.
     */
    override suspend fun logoutUser(issuer: String): ApiResponse<Unit> {
        return client.post("/v2/admin/auth/user/logout") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("issuer" to issuer))
        }.body()
    }

    /**
     * Retrieves the client information.
     *
     * @return An ApiResponse object containing the ClientInfo data.
     */
    override suspend fun getClientInfo(): ApiResponse<ClientInfo> =
        client.get("/v1/admin/client/get").body()
}

/**
 * Interface defining the endpoints for Magic Admin operations.
 */
interface MagicAdminEndpoints {

    /**
     * Retrieves the user information based on the provided issuer and optional wallet type.
     *
     * @param issuer The unique identifier (issuer) of the user.
     * @param walletType Optional parameter specifying the type of wallet associated with the user.
     * @return An ApiResponse object containing the UserInfo data.
     */
    suspend fun getUserInfo(issuer: String, walletType: String? = null): ApiResponse<UserInfo>

    /**
     * Logs out a user based on the provided issuer identifier.
     *
     * @param issuer The unique identifier (issuer) of the user to be logged out.
     * @return An ApiResponse object indicating the outcome of the logout operation.
     */
    suspend fun logoutUser(issuer: String): ApiResponse<Unit>

    /**
     * Retrieves the client information.
     *
     * @return An ApiResponse object containing the ClientInfo data.
     */
    suspend fun getClientInfo(): ApiResponse<ClientInfo>
}