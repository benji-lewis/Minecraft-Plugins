import java.net.URI
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher

group = "uk.co.xfour.kimjongun3"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.nova)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases/")
}

dependencies {
    implementation(libs.nova)
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val generatedResourcePackDir = layout.buildDirectory.dir("generated/resourcepack")

sourceSets {
    main {
        resources {
            srcDir(generatedResourcePackDir)
        }
    }
}

tasks.processResources {
}

tasks.test {
    useJUnitPlatform()
}

val proxyUri = System.getenv("HTTPS_PROXY") ?: System.getenv("HTTP_PROXY")
if (!proxyUri.isNullOrBlank()) {
    val parsedProxy = URI(proxyUri)
    val proxyHost = parsedProxy.host
    val proxyPort = if (parsedProxy.port != -1) parsedProxy.port else 80
    if (!proxyHost.isNullOrBlank()) {
        val proxyArgs = listOf(
            "-Dhttp.proxyHost=$proxyHost",
            "-Dhttp.proxyPort=$proxyPort",
            "-Dhttps.proxyHost=$proxyHost",
            "-Dhttps.proxyPort=$proxyPort"
        )
        val baseLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        tasks.matching { it.name == "_oriApplyBinDiff" }.configureEach {
            doFirst {
                val scriptFile = layout.buildDirectory.file("proxy-java/bin/java").get().asFile
                scriptFile.parentFile.mkdirs()
                val targetJava = baseLauncher.get().executablePath.asFile.absolutePath
                scriptFile.writeText(
                    """
                    #!/usr/bin/env bash
                    exec "$targetJava" ${proxyArgs.joinToString(" ")} "$@"
                    """.trimIndent()
                )
                scriptFile.setExecutable(true)
                @Suppress("UNCHECKED_CAST")
                val javaLauncherProperty = this::class.java.methods
                    .firstOrNull { it.name == "getJavaLauncher" }
                    ?.invoke(this) as? Property<JavaLauncher>
                javaLauncherProperty?.set(object : JavaLauncher {
                    override fun getExecutablePath() = layout.file(provider { scriptFile }).get()

                    override fun getMetadata() = baseLauncher.get().metadata
                })
            }
        }
    }
}


addon {
    name = "Kim Jong Un 3"
    version = project.version.toString()
    main = "uk.co.xfour.kimjongun3.KimJongUn3Addon"
    pluginMain = "uk.co.xfour.kimjongun3.KimJongUn3Plugin"
    description = "Adds a themed Nova addon mob that drops missile and launchpad parts with a dramatic launch."
    authors.add("Xfour")
    prefix = "KimJongUn3"

    val outDir = project.findProperty("outDir")
    if (outDir is String) {
        destination = File(outDir)
    }
}
