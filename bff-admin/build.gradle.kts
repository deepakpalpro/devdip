plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-validation")

    implementation(project(":module-identity"))
    implementation(project(":module-form-definition"))
    implementation(project(":module-form-import"))
    implementation(project(":module-submission"))
    implementation(project(":module-processing"))
    implementation(project(":module-pipeline"))
}
