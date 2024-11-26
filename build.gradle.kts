plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.web3j:core:5.0.0")
    api("io.ktor:ktor-client-core:3.0.1")
    api("io.ktor:ktor-client-java:3.0.1")
    api("io.ktor:ktor-client-content-negotiation:3.0.1")
    api("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:3.0.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.10")
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "com.skunkworks"
            artifactId = "magic-admin-kotlin"
            version = "1.0.2"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Executioner1939/magic-admin-kotlin")
            credentials {
                username = System.getenv("ACTOR")
                password = System.getenv("TOKEN")
            }
        }
    }
}