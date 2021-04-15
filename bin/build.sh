#!/usr/bin/env sh

rm -rf resources/public/css
sass --style=compressed src/scss/main.scss resources/public/css/main.css
rm -rf resources/public/js
npx shadow-cljs compile ui

rm -rf classes
mkdir classes
rm -rf target
clj -e "(compile 'com.ben-allred.audiophile.api.server)"
LOG_LEVEL=warn clj -A:uberjar -m uberdeps.uberjar --level warn --target target/audiophile.jar --main-class com.ben_allred.audiophile.api.server
