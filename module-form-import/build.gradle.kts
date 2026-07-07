plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-json")

    implementation(project(":module-identity"))
    implementation(project(":module-form-definition"))

    // Deterministic, in-JVM source extractors (no data leaves the platform):
    implementation("org.apache.pdfbox:pdfbox:3.0.3") // PDF (text + AcroForm)
    implementation("org.apache.poi:poi-ooxml:5.3.0") // XLS / XLSX spreadsheets
    implementation("org.jsoup:jsoup:1.18.1") // HTML form parsing
}
