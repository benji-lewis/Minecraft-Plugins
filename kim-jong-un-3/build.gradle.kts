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
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
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
