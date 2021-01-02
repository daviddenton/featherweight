#!/usr/bin/env bash

set -e

./gradlew clean shadowJar
docker build .
