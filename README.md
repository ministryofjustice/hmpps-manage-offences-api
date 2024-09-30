# Manage offences API
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-manage-offences-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-manage-offences-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-offences-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-manage-offences-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-manage-offences-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-manage-offences-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://manage-offences-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)


This is the API service for Managing Offences.

# Dependencies
This service requires a postgresql database.

# Building the project
Tools required:
* JDK v21+
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
1. `docker compose -f docker-compose-test.yml pull`
2. `docker compose -f docker-compose-test.yml up --no-start`
3. `docker compose -f docker-compose-test.yml start hmpps-auth manage-offences-db`
4. `./run-local.sh`

## Connecting to the SDRS staging API

### keytool usage
Use the keytool command. if modifying cacerts pass in the option `-cacerts`, otherwise the following can modify any truststore `-keystore /etc/ssl/certs/java/cacerts`

### add cert
The SDRS Staging api is publicly open api with root URL at  
https://sdrs.staging.apps.hmcts.net/
However, in order for it to work with java you have to add it to the truststore, locally this can be achieved by doing the following (using the default password of changeit):
1. Download the cert from https://sdrs.staging.apps.hmcts.net/ using a browser or the following command:  
`openssl s_client -servername sdrs.staging.apps.hmcts.net -connect crime-reference-data-api.staging.service.justice.gov.uk:443 </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' >sdrs-staging.pem`
2. Then add it to the tuststore using the following command:  
`keytool -noprompt -storepass changeit -importcert -trustcacerts -cacerts -file sdrs-staging.pem -alias sdrs_staging_cert`

The SDRS staging API is only used by the DEV environment. 

The SDRS Production API (used in preprod and prod) is not affected by this issue as the production certificate is trusted by the java trust store 

### List all cert in the trust store
`keytool -noprompt -storepass changeit -list -v -cacerts`

#### Delete a certificate from the truststore
`keytool -noprompt -storepass changeit -delete -alias sdrs_staging_cert -cacerts`