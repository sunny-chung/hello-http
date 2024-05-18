#!/bin/bash

set -e

./gradlew :test-server:bootJar

GRADLE_OPTS='-Xmx64m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs="-Xmx500m"' ./gradlew :test-server:bootRun &
TEST_SERVER_PID=$!

function cleanup {
  [ "$TEST_SERVER_PID" ] && kill $TEST_SERVER_PID
}

trap cleanup EXIT

READY=false
for attempt in {1..30}; do sleep 1; if curl -sI --fail-early -f http://localhost:18081/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server to be UP"; done
[ "$READY" = true ] || (echo "Test Server is not UP" && exit 1)
echo "Test Server is UP"

export GRADLE_OPTS='-Xmx64m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs="-Xmx2048m"'
./gradlew check -PisCI=true
