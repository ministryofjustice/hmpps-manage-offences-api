plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.7"
  kotlin("plugin.spring") version "2.2.21"
  kotlin("plugin.jpa") version "2.2.21"
  id("se.patrikerdes.use-latest-versions") version "0.2.19"
}

tasks.withType<Test> {
  environment("AWS_REGION", "eu-west-1")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.4.0")
    mavenBom("software.amazon.awssdk:bom:2.37.3")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  // AppInsights
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.security:spring-security-config")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")

  // Schedule locking
  implementation("net.javacrumbs.shedlock:shedlock-spring:6.10.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.10.0")

  // AWS
  // See comment below relating to the SQS lib, keep these AWS dependencies above the SQS dependency
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:sts")
  implementation("software.amazon.awssdk:netty-nio-client")

  // SQS
  // During an upgrade PR a springboot/aws related issue occurred which implied there was a conflict with the hmpps-sqs library
  // Moving this SQS lib below the AWS libs solved the problem. Not exactly sure why! See the PR for more details
  // https://github.com/ministryofjustice/hmpps-manage-offences-api/pull/175
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.1")

  // Miscellaneous
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")

  // Test dependencies
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.35")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.55.0")
  testImplementation("org.testcontainers:postgresql:1.21.3")
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
