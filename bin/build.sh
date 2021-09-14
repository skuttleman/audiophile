#!/usr/bin/env sh

$(dirname ${BASH_SOURCE[0]})/build-ui.sh

echo "[building uberjar ...]"
rm -rf classes
mkdir classes
rm -f target/audiophile.jar
clj -Sthreads 1 -e "(compile 'com.ben-allred.audiophile.backend.core)"
LOG_LEVEL=warn clj -Sthreads 1 -A:uberjar -m uberdeps.uberjar --level warn --target target/audiophile.jar --main-class com.ben_allred.audiophile.backend.core
echo "[... uberjar built]"
