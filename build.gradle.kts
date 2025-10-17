plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

tasks.withType<Test> {
  environment("AWS_REGION", "eu-west-1")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.3.1")
    mavenBom("software.amazon.awssdk:bom:2.31.54")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.6")

  // AppInsights
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.16.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.security:spring-security-config")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.10")

  // Schedule locking
  implementation("net.javacrumbs.shedlock:shedlock-spring:6.8.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.8.0")

  // AWS
  // See comment below relating to the SQS lib, keep these AWS dependencies above the SQS dependency
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:sts")
  implementation("software.amazon.awssdk:netty-nio-client")

  // SQS
  // During an upgrade PR a springboot/aws related issue occurred which implied there was a conflict with the hmpps-sqs library
  // Moving this SQS lib below the AWS libs solved the problem. Not exactly sure why! See the PR for more details
  // https://github.com/ministryofjustice/hmpps-manage-offences-api/pull/175
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.5")

  // Miscellaneous
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")

  // Test dependencies
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.29")
  testImplementation("org.wiremock:wiremock-standalone:3.13.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.50.0")
  testImplementation("org.testcontainers:postgresql:1.21.1")
  testImplementation(kotlin("test"))
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
