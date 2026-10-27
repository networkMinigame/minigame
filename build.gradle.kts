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

// IntelliJ가 Gradle로 main()을 실행할 때도 이 설정을 탄다. Windows 한국어 기본(코드 페이지 949)에서는
// 콘솔 출력이 MS949로 나가고 IntelliJ는 UTF-8로 읽어서 한글이 깨지므로, 어느 PC에서든 UTF-8로 고정한다.
tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "networkgame.Main"
}
