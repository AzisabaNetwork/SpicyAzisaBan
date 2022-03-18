dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("xyz.acrylicstyle.util:all:0.16.6") {
        exclude("xyz.acrylicstyle.util", "maven")
    }
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.3")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    shadowJar {
        manifest {
            attributes(
                "Main-Class" to "net.azisaba.spicyAzisaBan.cli.CLIMain",
            )
        }

        archiveFileName.set("SpicyAzisaBan-cli-${project.version}.jar")
    }
}
