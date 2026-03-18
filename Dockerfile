ARG BASE_IMAGE=ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy
FROM --platform=$BUILDPLATFORM ${BASE_IMAGE} AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

WORKDIR /app
ADD . .
USER root
RUN chown -R 1000:1000 /app
USER 1000
ENV GRADLE_USER_HOME=/app/.gradle
RUN ./gradlew --no-daemon assemble

FROM ${BASE_IMAGE}
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

USER root
RUN apt-get update && \
    apt-get -y upgrade && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

# Install AWS RDS Root cert into Java truststore
RUN mkdir -p /home/appuser/.postgresql && chown -R 2000:2000 /home/appuser/.postgresql
ADD --chown=2000:2000 https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem /home/appuser/.postgresql/root.crt

WORKDIR /app
COPY --from=builder --chown=2000:2000 /app/build/libs/hmpps-manage-offences-api*.jar /app/app.jar
COPY --from=builder --chown=2000:2000 /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=builder --chown=2000:2000 /app/applicationinsights.json /app
COPY --from=builder --chown=2000:2000 /app/applicationinsights.dev.json /app

COPY --chown=2000:2000 certificates/sdrs-staging.pem /app
RUN keytool -noprompt -storepass changeit -importcert -trustcacerts -cacerts -file sdrs-staging.pem -alias sdrs_staging_cert

USER 2000

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]