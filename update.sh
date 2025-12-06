#!/bin/bash
./gradlew --refresh-dependencies dependencyUpdates -Drevision=release --no-parallel
./gradlew --stop
