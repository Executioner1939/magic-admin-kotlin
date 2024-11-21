package com.skunkworks.magic.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing the information of a user.
 *
 * @property email The email address of the user.
 * @property issuer The unique identifier (issuer) of the user.
 * @property publicAddress The public blockchain address of the user.
 * @property wallets A list of Wallet objects associated with the user. Default value is null.
 */
@Serializable
data class UserInfo(
    val email: String,
    val issuer: String,
    @SerialName("public_address")
    val publicAddress: String,
    val wallets: List<Wallet>? = null
)