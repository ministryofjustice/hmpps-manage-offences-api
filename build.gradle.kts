plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.4"
  kotlin("plugin.spring") version "1.8.21"
  kotlin("plugin.jpa") version "1.8.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.0.0")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.4")

  // AppInsights
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.23.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.11.5")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

  implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.3.2")

  // Schedule locking
  implementation("net.javacrumbs.shedlock:shedlock-spring:5.1.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.1.0")

  // SQS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.0")

  // AWS
  implementation("software.amazon.awssdk:sts:2.20.65")
  implementation("software.amazon.awssdk:netty-nio-client:2.20.65")
  implementation("software.amazon.awssdk:sns:2.20.65")
  implementation("software.amazon.awssdk:s3:2.20.65")

  // Miscellaneous
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")

  // Test dependencies
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.3")
  testImplementation("com.h2database:h2")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.23.1")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
