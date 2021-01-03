#!/usr/bin/env bash

set -e

echo Building HitchhikersGuide...
./gradlew clean shadowJar

echo Packaging HitchhikersGuide...
docker build . -t hitchhikersguide

echo Running HitchhikersGuide...
docker run --env-file hitchhikersguide.env -p 8080:8080 hitchhikersguide
