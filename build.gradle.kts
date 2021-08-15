plugins {
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

group = "net.azisaba"
version = "0.0.17"

java {
    withJavadocJar()
    withSourcesJar()
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
    implementation(kotlin("stdlib"))
    implementation("xyz.acrylicstyle:java-util-kotlin:0.15.4")
    implementation("xyz.acrylicstyle:sequelize4j:0.5.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.3")
    implementation("xyz.acrylicstyle:minecraft-util:0.5.3")
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    test {
        useJUnitPlatform()
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
