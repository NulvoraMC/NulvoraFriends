plugins {
    java
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(21)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation(project(":common"))
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {
    jar {
        archiveBaseName.set("nulvora-friends-papermc")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "nulvora-friends-papermc"
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
