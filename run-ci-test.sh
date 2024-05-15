#!/bin/bash

set -e

./gradlew :test-server:bootJar
./gradlew :test-server:bootRun &

READY=false
for attempt in {1..20}; do sleep 1; if curl -sI --fail-early -f http://localhost:18081/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server to be UP"; done
[ "$READY" = true ] || (echo "Test Server is not UP" && exit 1)
echo "Test Server is UP"

./gradlew check -PisCI=true
