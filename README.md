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

# Running the service locally using run-local.sh
## Start the postgres database and auth
This will run the service locally. It starts the database and auth using docker compose then runs manage-offences-api via a bash script.
Run the following commands from the root directory of the project:
1. docker-compose -f docker-compose-test.yml pull
2. docker-compose -f docker-compose-test.yml up --no-start
3. docker-compose -f docker-compose-test.yml start hmpps-auth manage-offences-db
4. ./run-local.sh