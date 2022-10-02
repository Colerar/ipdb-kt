# <h1 align="center">ipdb-kt</h1>

<center><h3>An IPDB parser for Kotlin</h3></center>

<!--Badges-->

<p align="center">
<a href="https://kotlinlang.org"><img 
src="https://img.shields.io/badge/Kotlin-%230095D5.svg?style=for-the-badge&logo=kotlin&logoColor=white" 
alt="Kotlin"/></a><a 
href="https://gradle.org/"><img 
src="https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white" 
alt="Gradle"/></a><a 
href="https://www.jetbrains.com/idea/"><img 
src="https://img.shields.io/badge/IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white" 
alt="IntelliJ IDEA"/></a>
</p>

<p align="center">
<a 
href="https://www.apache.org/licenses/LICENSE-2.0"><img 
src="https://img.shields.io/badge/License-Apache2.0-lightgreen?style=for-the-badge&logo=opensourceinitiative&logoColor=white" 
alt="Apache 2.0 Open Source License"/></a><a 
href="https://s01.oss.sonatype.org/content/repositories/releases/moe/sdl/ipdb/"><img 
src="https://img.shields.io/maven-central/v/moe.sdl.ipdb/ipdb-core?style=for-the-badge" 
alt="Maven Developer"/></a></p>

## Install

Gradle (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("moe.sdl.ipdb:ipdb-core:<latest-version>")
}
```

## Usage

```kotlin
public fun main(): Unit = runBlocking {
    val reader = Reader(File("/path/to/file.ipdb"))
    println(reader.findThenParse(FullInfo, IPAddressString("1.1.1.1").toAddress(), "EN"))
}
```
