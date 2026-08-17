plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-source", "21", "-target", "21"))
}

dependencies {
    implementation(project(":common"))

    compileOnly("com.velocitypowered:velocity-api:4.0.0")
    annotationProcessor("com.velocitypowered:velocity-api:4.0.0")

    implementation("net.dv8tion:JDA:6.5.0") {
        exclude(module = "opus-java")
        exclude(module = "trove4j")
    }

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.spongepowered:configurate-hocon:4.1.2")
}

tasks {
    jar {
        archiveBaseName.set("nulvora-friends-velocity")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    shadowJar {
        archiveBaseName.set("nulvora-friends-velocity")
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        relocate("net.dv8tion", "com.nulvora.friends.shade.net.dv8tion")
        relocate("okhttp3", "com.nulvora.friends.shade.okhttp3")
        relocate("okio", "com.nulvora.friends.shade.okio")
        relocate("com.zaxxer.hikari", "com.nulvora.friends.shade.hikari")
        relocate("org.mariadb", "com.nulvora.friends.shade.mariadb")
        relocate("com.google.gson", "com.nulvora.friends.shade.gson")
        relocate("org.spongepowered.configurate", "com.nulvora.friends.shade.configurate")
    }

    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifact(tasks.shadowJar)
            groupId = project.group.toString()
            artifactId = "nulvora-friends-velocity"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "danielmaldonadodev/nulvorafriends"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "danielmaldonadodev"
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
