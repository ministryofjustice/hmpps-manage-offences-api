# Manage offences API
This is the API service for Managing Offences.

# Dependencies
This service requires a postgresql database.

# Building the project
Tools required:
* JDK v17+
* Kotlin
* docker
* docker-compose

# Running the unit tests

Unit tests mock all external dependencies and can be run with no dependent containers.

`$ ./gradlew test`

# Running the integration tests

Integration tests use Wiremock to stub any API calls required, and use a local H2 database
that is seeded with data specific to each test suite.

`$ ./gradlew integrationTest`

# Linting

`$ ./gradlew ktlintcheck`

# OWASP Dependency Checking scanning

`$ ./gradlew dependencyCheckAnalyze`
