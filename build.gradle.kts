import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.5.30"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

group = "net.azisaba"
version = "0.0.37-${getBranch()}-${getGitHash()}"

java {
    withJavadocJar()
    withSourcesJar()
}

fun getBranch(): String =
    file("./.git/HEAD")
        .readText()
        .replace("^.*: (.*)$".toRegex(), "$1")
        .trim(' ', '\n')
        .split('/')
        .last()

fun getGitHash(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        val ref = file("./.git/HEAD").readText().replace("^.*: (.*)$".toRegex(), "$1").trim(' ', '\n')
        println("Reading file ${file("./.git/$ref").absolutePath}")
        file("./.git/$ref").readText().trim(' ', '\n').substring(0..7)
    }
}

fun hasUncommittedChanges(): Boolean {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "status", "--porcelain")
            standardOutput = stdout
        }
        stdout.toString().trim().isNotBlank()
    } catch (e: Exception) {
        false
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
    skip()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo2.acrylicstyle.xyz") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")
    implementation("xyz.acrylicstyle:java-util-kotlin:0.15.4")
    implementation("xyz.acrylicstyle:sequelize4j:0.5.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.3")
    implementation("xyz.acrylicstyle:minecraft-util:0.5.3")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    test {
        useJUnitPlatform()
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name,
                "debugBuild" to hasUncommittedChanges().toString(),
                "devBuild" to (getBranch() != "main").toString(),
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(projectDir) { include("LICENSE") }
    }

    shadowJar {
        relocate("kotlin", "net.azisaba.spicyAzisaBan.libs.kotlin")
        relocate("util", "net.azisaba.spicyAzisaBan.libs.util")
        relocate("xyz.acrylicstyle.sql", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.sql")
        relocate("xyz.acrylicstyle.mcutil", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.mcutil")
        relocate("net.blueberrymc.native_util", "net.azisaba.spicyAzisaBan.libs.net.blueberrymc.native_util")
        relocate("org.mariadb", "net.azisaba.spicyAzisaBan.libs.org.mariadb")

        minimize()
        archiveFileName.set("SpicyAzisaBan-${project.version}.jar")
    }
}

println("Deleting cached bungee.yml")
file("./build/resources/main/bungee.yml").apply {
    if (exists()) delete()
}
println("Version: ${project.version}")
println("Debug build: ${hasUncommittedChanges()}")
println("Dev build: ${project.version.toString().contains("-dev")}")
