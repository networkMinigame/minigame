plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("com.google.code.gson:gson:2.15.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "networkgame.Main"
}
