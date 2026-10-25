plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("com.formdev:flatlaf-extras:3.7.2")
    implementation("com.formdev:flatlaf-intellij-themes:3.7.2")
    implementation("com.miglayout:miglayout-swing:11.5.2")
    implementation("com.google.code.gson:gson:2.15.0")
    implementation("tools.jackson.core:jackson-databind:3.2.4")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "networkgame.Main"
}
