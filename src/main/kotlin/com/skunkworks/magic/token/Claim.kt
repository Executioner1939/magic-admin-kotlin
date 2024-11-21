package com.skunkworks.magic.token

import kotlinx.serialization.Serializable

/**
 * Represents a claim within a token which includes various details such as issuance time, expiration time, issuer, and subject.
 *
 * @property issuedAt The timestamp when the token was issued.
 * @property expiresAt The timestamp when the token expires.
 * @property issuer The identifier of the entity that issued the token.
 * @property subject The subject identifier that the token pertains to.
 * @property audience The intended audience for the token.
 * @property notBefore The timestamp before which the token is not valid.
 * @property tokenId The unique identifier for the token.
 * @property additional Optional additional information associated with the token.
 */
@Serializable
data class Claim(
    val iat: Long,
    val ext: Long,
    val iss: String,
    val sub: String,
    val aud: String,
    val nbf: Long,
    val tid: String,
    val add: String? = null
)