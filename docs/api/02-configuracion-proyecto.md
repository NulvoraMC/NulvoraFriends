# 2. Configuracion del proyecto

Tu extension es un plugin PaperMC normal. La unica dependencia especial es `nulvora-friends-papermc`.

## 2.1 Repositorio Maven

Los artifacts de NulvoraFriends se publican en un repositorio Maven propio (Nexus/Sonatype):
`https://maven.elordenador.org/repository/maven-releases/`. Las releases (sin sufijo `-SNAPSHOT`,
p.ej. `1.3.3`) se resuelven por lectura anonima, sin necesidad de credenciales ni token.

> Si tu instalacion de NulvoraFriends es una `-SNAPSHOT` interna (no publicada), pregunta al
> mantenedor de tu red por el repositorio `maven-snapshots` equivalente.

## 2.2 Configuracion de Gradle

### settings.gradle.kts

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.elordenador.org/repository/maven-releases/")
    }
}

rootProject.name = "mi-extension"
```

### build.gradle.kts completo

```kotlin
plugins {
    java
}

group = "com.ejemplo"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Solo si NO usas dependencyResolutionManagement en settings.gradle.kts:
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.elordenador.org/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.3.3")
}
```

> **IMPORTANTE**: La dependencia de NulvoraFriends es `compileOnly` porque el JAR ya existe en el servidor. **NUNCA** uses `implementation`.

### Por que `compileOnly`?

- `compileOnly` = "necesito esta clase para compilar, pero ya estara disponible en runtime"
- `implementation` = "empaqueto esta clase en mi JAR" ← **NO hacer esto**
- Si usas `implementation`, tendras conflictos de clases con la instalacion de NulvoraFriends en el servidor

## 2.3 Configuracion de Maven

Si usas Maven en vez de Gradle:

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ejemplo</groupId>
    <artifactId>mi-extension</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>nulvorafriends-releases</id>
            <url>https://maven.elordenador.org/repository/maven-releases/</url>
            <releases><enabled>true</enabled></releases>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.1-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.nulvora.friends</groupId>
            <artifactId>nulvora-friends-papermc</artifactId>
            <version>1.3.3</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

> En Maven, `scope: provided` equivale a `compileOnly` de Gradle. No hace falta configurar
> `~/.m2/settings.xml` para este repositorio (lectura anonima).

## 2.4 plugin.yml

```yaml
name: MiExtension
version: '1.0.0'
main: com.ejemplo.miextension.MiExtension
api-version: '1.21'
depend: [NulvoraFriends]
```

### Por que `depend` y no `softdepend`?

- `depend` = NulvoraFriends **debe** cargar antes que tu plugin. Garantiza que la API este disponible.
- `softdepend` = NulvoraFriends carga despues si no esta. Tu plugin podria intentar usar la API y fallar.

Usa **siempre** `depend` para esta extension.

## 2.5 Verificar la configuracion

```bash
# Gradle
./gradlew dependencies --configuration compileClasspath

# Maven
mvn dependency:tree
```

Deberias ver algo como:

```
com.nulvora.friends:nulvora-friends-papermc:1.3.3 -> compileOnly (provided)
```

Si no aparece, revisa la URL del repositorio y que la version exista en
`https://maven.elordenador.org/repository/maven-releases/`.

## Siguiente paso

Continua con la [Implementacion paso a paso](03-implementacion-paso-a-paso.md).
