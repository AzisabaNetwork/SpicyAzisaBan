repositories {
    maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/") }
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    kapt("com.velocitypowered:velocity-api:3.0.1")
    testImplementation("com.velocitypowered:velocity-api:3.0.1")
}

tasks {
    shadowJar {
        archiveFileName.set("SpicyAzisaBan-Velocity-${project.version}.jar")
    }
}
