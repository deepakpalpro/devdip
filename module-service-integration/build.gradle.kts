plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")

    // Jackson for building/parsing Ollama JSON payloads (also transitively via module-form-import).
    implementation("org.springframework.boot:spring-boot-starter-json")

    // External/AI form-import providers implement the extractor SPI defined in module-form-import.
    implementation(project(":module-form-import"))
}
