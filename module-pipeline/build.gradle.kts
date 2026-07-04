plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-json")

    implementation(project(":module-form-definition"))
    implementation(project(":module-submission"))
    implementation(project(":module-processing"))
    implementation(project(":module-transformation"))
    implementation(project(":module-downstream"))
    implementation(project(":module-notification"))
}
