plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.6"
  kotlin("plugin.spring") version "1.6.21"
  kotlin("plugin.jpa") version "1.6.21"
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

  // Problem with hibernate-core 5.6.7.Final which was causing issue with 'startingWith'
  // https://github.com/spring-projects/spring-data-jpa/issues/2472
  // https://hibernate.atlassian.net/browse/HHH-15142
  // TODO Temporarily revert to 5.6.5.Final until fixed
  implementation("org.hibernate:hibernate-core:5.6.5.Final")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.3.4")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-ui:1.6.8")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.8")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.8")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.0.32")
  testImplementation("com.h2database:h2")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
