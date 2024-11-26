# Magic Admin Kotlin SDK

The Magic Admin Kotlin SDK provides convenient ways for developers to interact with Magic API endpoints and an array of utilities to handle [DID Tokens](https://magic.link/docs/introduction/decentralized-id).

## Table of Contents

- [Documentation](#documentation)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Development](#development)

## Documentation

See the [Magic documentation](https://magic.link/docs) for detailed information on how to use the Magic Admin SDK.

## Installation

The SDK requires Kotlin and is compatible with JVM-based projects. You can include it in your project by adding the dependency to your build file.

### Gradle

Add the following to your `build.gradle` file:

```groovy
dependencies {
    implementation 'com.skunkworks.magic:magic-admin-kotlin:1.0.0'
}
```

```xml
<dependencies>
    <dependency>
        <groupId>com.skunkworks.magic</groupId>
        <artifactId>magic-admin-kotlin</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

# Quick Start
Before you start, you will need an API secret key. You can get this from the Magic Dashboard. Once you have it, you can instantiate a MagicAdminClient object.

Retrieve User Info by DID Token
Sample code to retrieve user info by a DID Token:

```kotlin
import com.skunkworks.magic.api.MagicAdminClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val magicClient = MagicAdminClient(
        secretKey = "<YOUR_API_SECRET_KEY>"
    )

    val userInfoResult = magicClient.getUserMetadataByToken("<DID_TOKEN>")
    userInfoResult.onSuccess { userInfo ->
        println("User Info: $userInfo")
    }.onFailure { error ->
        println("Error: ${error.message}")
    }
}
}
```

## Validate DID Token and Retrieve Claim and Proof
Sample code to validate a DID Token and retrieve the claim and proof from the token:

```kotlin
import com.skunkworks.magic.token.Token

fun main() {
    val didToken = "<DID_TOKEN>"

    try {
        val token = Token.decode(didToken)
        token.validate()

        println("Token is valid")
        println("Claim: ${token.claim}")
        println("Proof: ${token.proof}")
    } catch (e: Exception) {
        println("DID Token is invalid: ${e.message}")
    }
}
```

## Configure Network Strategy
The MagicAdminClient allows you to customize the HTTP client configuration, including timeouts and base URL:

```kotlin
import com.skunkworks.magic.api.MagicAdminClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val customHttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }
    val magicClient = MagicAdminClient(
        secretKey = "<YOUR_API_SECRET_KEY>",
        httpClient = customHttpClient
    )

    // Use magicClient as needed
}
```

# Development
We welcome contributions to the SDK. To get started, clone this repository and set up your development environment.

# Running Tests
To run the existing tests:

```shell
./gradlew test
```

# Building the Project
To build the project:

```shell
./gradlew build
```

Changelog
See Changelog for a history of changes.