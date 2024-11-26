plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("maven-publish")
}

group = "com.skunkworks"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.web3j:core:4.12.2")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-java:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.10")
    testImplementation("io.mockk:mockk:1.13.8")
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
            groupId = "com.skunkworks.magic"
            artifactId = "magic-admin-kotlin"
            version = "1.0.1"
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