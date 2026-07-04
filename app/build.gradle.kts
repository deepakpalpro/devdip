plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":bff-consumer"))
    implementation(project(":bff-admin"))
    implementation(project(":module-identity"))
    implementation(project(":module-form-definition"))
    implementation(project(":module-form-import"))
    implementation(project(":module-submission"))
    implementation(project(":module-discovery"))
    implementation(project(":module-processing"))
    implementation(project(":module-transformation"))
    implementation(project(":module-service-integration"))
    implementation(project(":module-downstream"))
    implementation(project(":module-notification"))
    implementation(project(":module-analytics"))
    implementation(project(":module-observability"))
    implementation(project(":module-pipeline"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("banking-forms-platform.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}
