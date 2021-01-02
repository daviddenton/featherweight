#!/usr/bin/env bash

set -e

echo Building Featherweight...
./gradlew clean shadowJar

echo Packaging Featherweight...
docker build . -t featherweight

echo Running Featherweight...
docker run --env-file featherweight.env -p 8080:8080 featherweight
