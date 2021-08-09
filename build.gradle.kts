plugins {
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "net.azisaba"
version = "0.0.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo2.acrylicstyle.xyz") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("xyz.acrylicstyle:java-util-kotlin:0.15.4")
    implementation("xyz.acrylicstyle:sequelize4j:0.4.6")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.3")
    implementation("xyz.acrylicstyle:minecraft-util:0.5.3")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

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
