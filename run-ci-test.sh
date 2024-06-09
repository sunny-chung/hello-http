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

GRADLE_OPTS="$TEST_SERVER_OPTS" \
  ./gradlew :test-server:bootRun --args="--spring.profiles.active=h1-ssl" &
TEST_SERVER_H1_SSL_PID=$!

GRADLE_OPTS="$TEST_SERVER_OPTS" \
  ./gradlew :test-server:bootRun --args="--spring.profiles.active=ssl" &
TEST_SERVER_SSL_PID=$!

GRADLE_OPTS="$TEST_SERVER_OPTS" \
  ./gradlew :test-server:bootRun --args="--spring.profiles.active=mtls" &
TEST_SERVER_MTLS_PID=$!

function cleanup {
  set +e # on error resume next
  [ "$TEST_SERVER_PID" ] && kill $TEST_SERVER_PID
  [ "$TEST_SERVER_H1_PID" ] && kill $TEST_SERVER_H1_PID
  [ "$TEST_SERVER_H1_SSL_PID" ] && kill $TEST_SERVER_H1_SSL_PID
  [ "$TEST_SERVER_SSL_PID" ] && kill $TEST_SERVER_SSL_PID
  [ "$TEST_SERVER_MTLS_PID" ] && kill $TEST_SERVER_MTLS_PID
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

READY=false
for attempt in {1..60}; do sleep 1; if curl -sI --insecure --fail-early -f https://localhost:18088/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server (HTTP/1) to be UP"; done
[ "$READY" = true ] || (echo "Test Server (HTTP/1 SSL) is not UP" && exit 1)
echo "Test Server (HTTP/1 SSL) is UP"

READY=false
for attempt in {1..60}; do sleep 1; if curl -sI --insecure  --fail-early -f https://localhost:18084/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server (HTTP/1) to be UP"; done
[ "$READY" = true ] || (echo "Test Server (SSL) is not UP" && exit 1)
echo "Test Server (SSL) is UP"

READY=false
for attempt in {1..60}; do sleep 1; if curl -sI --insecure --cert ./test-common/src/main/resources/tls/clientKeyAndCert.pem --fail-early -f https://localhost:18086/actuator/health/; then READY=true; break; fi; echo "Waiting for Test Server (HTTP/1) to be UP"; done
[ "$READY" = true ] || (echo "Test Server (mTLS) is not UP" && exit 1)
echo "Test Server (mTLS) is UP"

export GRADLE_OPTS='-Xmx64m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs="-Xmx3072m -XX:+HeapDumpOnOutOfMemoryError"'
./gradlew check -PisCI=true
