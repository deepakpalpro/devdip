plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-json")

    // JavaMailSender for the SMTP email channel (auto-configured only when spring.mail.* is set).
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Recipient resolution reads the submission's section data via the submission + form modules.
    implementation(project(":module-submission"))
    implementation(project(":module-form-definition"))
}
