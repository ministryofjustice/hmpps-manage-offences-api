plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.2"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

tasks.withType<Test> {
  environment("AWS_REGION", "eu-west-1")
  useJUnitPlatform()
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.3.1")
    mavenBom("software.amazon.awssdk:bom:2.31.54")
    mavenBom("org.junit:junit-bom:5.11.4")
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.springframework.boot") {
      useVersion("3.4.1")
    }
    if (requested.group == "org.springframework") {
      useVersion("6.2.1")
    }
    if (requested.group == "org.springframework.security") {
      useVersion("6.4.2")
    }
    if (requested.group == "org.junit.platform" && requested.name == "junit-platform-launcher") {
      useVersion("1.11.4")
    }
    if (requested.group == "org.junit.platform" && requested.name == "junit-platform-engine") {
      useVersion("1.11.4")
    }
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Database
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // AppInsights
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.16.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

  // Spring Boot Starters
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.security:spring-security-config")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.8")

  // ShedLock
  implementation("net.javacrumbs.shedlock:shedlock-spring:6.8.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.8.0")

  // AWS
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:sts")
  implementation("software.amazon.awssdk:netty-nio-client")

  // SQS (Downgraded to 5.x for Spring Boot 3 compatibility)
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.3")

  // Misc
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

  // ****************
  // TEST DEPENDENCIES
  // ****************

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit") // removes JUnit4
  }

  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")

  // Kotlin test
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")

  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.29")
  testImplementation("org.wiremock:wiremock-standalone:3.13.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.50.0")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.apache.commons:commons-csv:1.9.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation(kotlin("test"))
}

kotlin {
  jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
}
