plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.nebulaaudio"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Config
    implementation("org.yaml:snakeyaml:2.2")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.0")

    // HTTP server + client (Javalin wraps Jetty; lightweight, embeddable, no servlet boilerplate)
    implementation("io.javalin:javalin:6.1.3")

    // WebSocket is provided by Javalin/Jetty transitively.

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Opus JNI bindings (native codec wrapper)
    implementation("com.github.jaredmdobson:concentus:1.1") // pure-java Opus fallback (no native lib needed)

    // HTTP client for source resolution
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("io.nebulaaudio.NebulaAudio")
}

tasks.shadowJar {
    archiveBaseName.set("NebulaAudio")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
