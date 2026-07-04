plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-json")

    // External/AI form-import providers implement the extractor SPI defined in module-form-import.
    implementation(project(":module-form-import"))

    // External/AI pipeline evaluators + service-call seam defined in module-pipeline.
    implementation(project(":module-pipeline"))

    // External delivery channels implement the NotificationChannel SPI defined in module-notification.
    implementation(project(":module-notification"))

    // Timeline events for service-call audit.
    implementation(project(":module-submission"))
}
