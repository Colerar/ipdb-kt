import java.util.*

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    `maven-publish`
    signing
}

group = "moe.sdl.ipdb"
version = "0.1.1"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    api("com.github.seancfoley:ipaddress:5.3.4")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

fun Project.getRootProjectLocalProps(): Map<String, String> {
    val file = project.rootProject.file("local.properties")
    return if (file.exists()) {
        file.reader().use {
            Properties().apply {
                load(it)
            }
        }.toMap().map {
            it.key.toString() to it.value.toString()
        }.toMap()
    } else emptyMap()
}

val secretPropsFile: File = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    val props = getRootProjectLocalProps()
    props.forEach { (t, u) -> extra[t] = u }
} else {
    extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    extra["signing.password"] = System.getenv("SIGNING_PASSWORD")
    extra["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    extra["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    extra["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

fun Project.getExtraString(name: String) = kotlin.runCatching { extra[name]?.toString() }.getOrNull()

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            artifactId = project.name
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set("ipdb-kt")
                description.set("An IPDB parser for Kotlin")
                url.set("https://github.com/Colerar/ipdb-kt")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("Colerar")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Colerar/ipdb-kt.git")
                    developerConnection.set("scm:git:ssh://github.com/Colerar/ipdb-kt.git")
                    url.set("https://github.com/Colerar/ipdb-kt")
                }
            }
        }

        repositories {
            maven {
                name = "sonatype"
                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                val url = if (version.toString().contains("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                setUrl(url)
                credentials {
                    username = getExtraString("ossrhUsername")
                    password = getExtraString("ossrhPassword")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenKotlin"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
