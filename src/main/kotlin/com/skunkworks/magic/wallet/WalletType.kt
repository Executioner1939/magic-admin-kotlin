package com.skunkworks.magic.wallet

/**
 * Enum class representing different types of wallets.
 *
 * This enum is used to specify and differentiate between various blockchain
 * networks and their corresponding wallets within the Magic platform.
 */
enum class WalletType {
    ETH,
    HARMONY,
    ICON,
    FLOW,
    TEZOS,
    ZILLIQA,
    POLKADOT,
    SOLANA,
    AVAX,
    ALGOD,
    COSMOS,
    CELO,
    BITCOIN,
    NEAR,
    HELIUM,
    CONFLUX,
    TERRA,
    TAQUITO,
    ED,
    HEDERA,
    ANY,
    NONE;

    override fun toString(): String = name
}