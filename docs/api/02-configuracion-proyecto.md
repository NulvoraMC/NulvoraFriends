# 2. Configuracion del proyecto

Tu extension es un plugin PaperMC normal. La unica dependencia especial es `nulvora-friends-papermc`.

## 2.1 Obtener acceso al repositorio Maven

Los artifacts de NulvoraFriends se publican en **GitHub Packages**. Para resolverlos necesitas un **Personal Access Token (Pasos para crear el token:

1. Ir a [https://github.com/settings/tokens](https://github.com/settings/tokens)
2. Click en **"Generate new token"** → **"Generate new token (classic)"**
3. Darle un nombre descriptivo (ej: `maven-packages`)
4. Seleccionar el scope **`read:packages`**
5. Click en **"Generate token"** y copiarlo

> **IMPORTANTE**: El token solo se muestra una vez. Guardalo en un lugar seguro.

## 2.2 Configuracion de credenciales

GitHub Packages requiere autenticacion. Tienes dos opciones:

### Opcion A: Variables de entorno (recomendado)

```bash
# Linux/Mac (en .bashrc, .zshrc o .env)
export GITHUB_ACTOR="tu-usuario-de-github"
export GITHUB_TOKEN="ghp_tu_token_aqui"
```

```powershell
# Windows (PowerShell)
$env:GITHUB_ACTOR="tu-usuario-de-github"
$env:GITHUB_TOKEN="ghp_tu_token_aqui"
```

### Opcion B: Archivo gradle.properties

Crear o editar `~/.gradle/gradle.properties`:

```properties
github.user=tu-usuario-de-github
github.token=ghp_tu_token_aqui
```

> **NUNCA** subas el token a un repositorio Git. Si usas CI/CD, usa secrets del repositorio.

## 2.3 Configuracion de Gradle

### settings.gradle.kts

Si configuras las credenciales via variables de entorno, necesitas declarar el repositorio en `settings.gradle.kts`:

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
        maven("https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends") {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("github.user") as? String ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("github.token") as? String ?: ""
            }
        }
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
    maven("https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends") {
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("github.user") as? String ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("github.token") as? String ?: ""
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.2.0-SNAPSHOT")
}
```

> **IMPORTANTE**: La dependencia de NulvoraFriends es `compileOnly` porque el JAR ya existe en el servidor. **NUNCA** uses `implementation`.

### Por que `compileOnly`?

- `compileOnly` = "necesito esta clase para compilar, pero ya estara disponible en runtime"
- `implementation` = "empaqueto esta clase en mi JAR" ← **NO hacer esto**
- Si usas `implementation`, tendras conflictos de clases con la instalacion de NulvoraFriends en el servidor

## 2.4 Configuracion de Maven

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
            <id>github-nulvorafriends</id>
            <url>https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends</url>
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
            <version>1.2.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

> En Maven, `scope: provided` equivale a `compileOnly` de Gradle.

### Credenciales Maven

Crear `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-nulvorafriends</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

## 2.5 plugin.yml

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

## 2.6 Verificar la configuracion

```bash
# Gradle
./gradlew dependencies --configuration compileClasspath

# Maven
mvn dependency:tree
```

Deberias ver algo como:

```
com.nulvora.friends:nulvora-friends-papermc:1.2.0-SNAPSHOT -> compileOnly (provided)
```

Si no aparece, revisa las credenciales y la configuracion del repositorio.

## Siguiente paso

Continua con la [Implementacion paso a paso](03-implementacion-paso-a-paso.md).
