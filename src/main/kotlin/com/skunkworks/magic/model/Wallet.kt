package com.skunkworks.magic.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a blockchain wallet.
 *
 * @property network The blockchain network of the wallet (e.g., Ethereum, Bitcoin).
 * @property publicAddress The public address of the wallet.
 * @property walletType The type of the wallet (e.g., hardware, software).
 */
@Serializable
data class Wallet(
    val network: String,
    @SerialName("public_address")
    val publicAddress: String,
    @SerialName("wallet_type")
    val walletType: String
)