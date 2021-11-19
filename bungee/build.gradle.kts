dependencies {
    implementation(project(":common"))
    compileOnly("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
    testImplementation("net.md-5:bungeecord-api:1.17-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("SpicyAzisaBan-Bungee-${project.version}.jar")
    }
}
