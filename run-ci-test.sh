#!/bin/bash

set -e

./gradlew :test-server:bootJar
./gradlew :test-server:bootRun &
TEST_SERVER_PID=$!

function cleanup {
  [ "$TEST_SERVER_PID" ] && kill $TEST_SERVER_PID
}

trap cleanup EXIT

READY=false
for attempt in {1..20}; do sleep 1; if curl -sI --fail-early -f http://localhost:18081/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server to be UP"; done
[ "$READY" = true ] || (echo "Test Server is not UP" && exit 1)
echo "Test Server is UP"

export JAVA_OPTS='"-Xmx2048m"'
export GRADLE_OPTS='"-Dorg.gradle.jvmargs=-Xmx2048m"'
./gradlew check -PisCI=true
