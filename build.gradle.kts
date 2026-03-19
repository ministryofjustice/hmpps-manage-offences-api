plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.6"
  kotlin("plugin.spring") version "2.3.20"
  kotlin("plugin.jpa") version "2.3.20"
  id("se.patrikerdes.use-latest-versions") version "0.2.19"
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
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.0")
    mavenBom("software.amazon.awssdk:bom:2.42.16")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.26.0")

  implementation("io.jsonwebtoken:jjwt-api:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.security:spring-security-config")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.16")

  implementation("net.javacrumbs.shedlock:shedlock-spring:7.7.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.7.0")

  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:sts")
  implementation("software.amazon.awssdk:netty-nio-client")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.1.0")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
  }
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.springframework.boot:spring-boot-webflux-test")

  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.39")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.60.1")
  testImplementation("org.testcontainers:postgresql:1.21.4")
  testImplementation("org.apache.commons:commons-csv:1.14.1")
}

kotlin {
  jvmToolchain(25)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
}
