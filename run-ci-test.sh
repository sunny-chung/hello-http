#!/bin/bash

set -e

./gradlew :test-server:bootJar

TEST_SERVER_OPTS='-Xmx64m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs="-Xmx400m"'
GRADLE_OPTS="$TEST_SERVER_OPTS" \
  ./gradlew :test-server:bootRun &
TEST_SERVER_PID=$!

GRADLE_OPTS="$TEST_SERVER_OPTS" \
  ./gradlew :test-server:bootRun --args="--spring.profiles.active=h1" &
TEST_SERVER_H1_PID=$!

function cleanup {
  set +e # on error resume next
  [ "$TEST_SERVER_PID" ] && kill $TEST_SERVER_PID
  [ "$TEST_SERVER_H1_PID" ] && kill $TEST_SERVER_H1_PID
}

trap cleanup EXIT

READY=false
for attempt in {1..90}; do sleep 1; if curl -sI --fail-early -f http://localhost:18081/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server to be UP"; done
[ "$READY" = true ] || (echo "Test Server is not UP" && exit 1)
echo "Test Server is UP"

READY=false
for attempt in {1..60}; do sleep 1; if curl -sI --fail-early -f http://localhost:18083/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server (HTTP/1) to be UP"; done
[ "$READY" = true ] || (echo "Test Server (HTTP/1) is not UP" && exit 1)
echo "Test Server (HTTP/1) is UP"

export GRADLE_OPTS='-Xmx64m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs="-Xmx3072m"'
./gradlew check -PisCI=true
