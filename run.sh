#!/bin/sh
exec java \
  -Djavax.net.ssl.trustStore=/app/trusted.jks \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=jks \
  -javaagent:/app/agent.jar \
  -jar /app/app.jar