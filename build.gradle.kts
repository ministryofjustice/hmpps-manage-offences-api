plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.6.0"
  kotlin("plugin.spring") version "1.9.10"
  kotlin("plugin.jpa") version "1.9.10"
  id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.0.0-RC2")
    mavenBom("software.amazon.awssdk:bom:2.18.28")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  // AppInsights
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.31.0")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:0.12.3")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

  implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.6.0")

  // Schedule locking
  implementation("net.javacrumbs.shedlock:shedlock-spring:5.9.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.9.0")

  // AWS
  // See comment below relating to the SQS lib, keep these AWS dependencies above the SQS dependency
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:sts")
  implementation("software.amazon.awssdk:netty-nio-client")

  // SQS
  // During a upgrade PR a springboot/aws related issue occurred which implied there was a conflict with the hmpps-sqs library
  // Moving this SQS lib below the AWS libs solved the problem. Not exactly sure why! See the PR for more details
  // https://github.com/ministryofjustice/hmpps-manage-offences-api/pull/175
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.1.1")

  // Miscellaneous
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")

  // Test dependencies
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.18")
  testImplementation("com.h2database:h2")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.31.0")
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
