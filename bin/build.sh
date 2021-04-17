#!/usr/bin/env sh

./build-ui.sh

echo "building uberjar"
rm -rf classes
mkdir classes
rm -rf target
clj -e "(compile 'com.ben-allred.audiophile.api.server)"
LOG_LEVEL=warn clj -A:uberjar -m uberdeps.uberjar --level warn --target target/audiophile.jar --main-class com.ben_allred.audiophile.api.server
echo "uberjar built"
