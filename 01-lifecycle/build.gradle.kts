plugins {
    application
    java
}

group = "dev.stefanjakic.architecture"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "dev.stefanjakic.lifecycle.LifecycleDemo"
}

tasks.test {
    useJUnitPlatform()
}
