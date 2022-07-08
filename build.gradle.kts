plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.3.2"
  kotlin("plugin.spring") version "1.7.10"
  kotlin("plugin.jpa") version "1.7.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.3.4")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.9")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.9")
  implementation("com.vladmihalcea:hibernate-types-52:2.14.0")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.0.32")
  testImplementation("com.h2database:h2")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
