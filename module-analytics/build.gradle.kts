plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-json")

    implementation(project(":module-submission"))
    implementation(project(":module-form-definition"))
    implementation(project(":module-pipeline"))
}
