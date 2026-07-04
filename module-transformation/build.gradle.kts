plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")

    implementation(project(":module-submission"))
}
