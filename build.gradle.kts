import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    `maven-publish`
}

group = "net.azisaba"
version = "0.0.37-${getBranch()}-${getGitHash()}${if (hasUncommittedChanges()) "-debug" else ""}"

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
    implementation("xyz.acrylicstyle.util:maven:0.16.5")
    implementation("net.blueberrymc:native-util:1.2.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("xyz.acrylicstyle.util:all:0.16.5") {
        exclude("com.google.guava", "guava")
        exclude("org.reflections", "reflections")
        exclude("org.json", "json")
        exclude("org.yaml", "snakeyaml")
        exclude("xyz.acrylicstyle.util", "maven")
    }
    implementation("xyz.acrylicstyle:sequelize4j:0.6.2") {
        exclude("xyz.acrylicstyle", "java-util-all")
    }
    implementation("xyz.acrylicstyle:minecraft-util:0.5.4") {
        exclude("xyz.acrylicstyle", "java-util-all")
    }
    compileOnly("org.mariadb.jdbc:mariadb-java-client:2.7.3")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "16" }
    compileTestKotlin { kotlinOptions.jvmTarget = "16" }

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
        dependencies {
            exclude(dependency("com.google.guava:guava:.*"))
            exclude(dependency("org.reflections:reflections:.*"))
            exclude(dependency("org.json:json:.*"))
            exclude(dependency("org.yaml:snakeyaml:.*"))
            exclude(dependency("com.google.code.findbugs:jsr305:.*"))
            exclude(dependency("org.javassist:javassist:.*"))
            exclude(dependency("org.slf4j:slf4j-api:.*"))
            exclude(dependency("com.google.guava:failureaccess:.*"))
            exclude(dependency("com.google.guava:listenablefuture:.*"))
            exclude(dependency("com.google.code.findbugs:jsr305:.*"))
            exclude(dependency("org.checkerframework:checker-qual:.*"))
            exclude(dependency("com.google.errorprone:error_prone_annotations:.*"))
            exclude(dependency("com.google.j2objc:j2objc-annotations:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:.*"))
            exclude(dependency("org.xerial.snappy:snappy-java:"))
            exclude(dependency("org.jetbrains:annotations:.*"))
        }
        relocate("kotlin", "net.azisaba.spicyAzisaBan.libs.kotlin")
        relocate("util", "net.azisaba.spicyAzisaBan.libs.util")
        relocate("xyz.acrylicstyle.sql", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.sql")
        relocate("xyz.acrylicstyle.mcutil", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.mcutil")
        relocate("org.mariadb", "net.azisaba.spicyAzisaBan.libs.org.mariadb")
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
