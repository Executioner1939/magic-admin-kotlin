package com.skunkworks.magic.token

import kotlinx.serialization.Serializable

/**
 * The `Claim` data class represents the claims component of a token. It holds various fields that
 * are used for token validation and authorization.
 *
 * @property iat Represents the issued-at timestamp of the token.
 * @property ext Represents the expiration timestamp of the token.
 * @property iss Represents the issuer of the token.
 * @property sub Represents the subject of the token.
 * @property aud Represents the intended audience of the token.
 * @property nbf Represents the not-before timestamp, indicating when the token becomes valid.
 * @property tid Represents a unique token identifier.
 * @property add Represents additional claims, which are optional.
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