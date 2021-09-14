#!/usr/bin/env sh

echo "[building ui ...]"
rm -rf node_modules
npm install
rm -rf resources/public/css
sass --style=compressed src/scss/main.scss resources/public/css/main.css
rm -rf resources/public/js
clj -A:shadow-cljs -Sthreads 1 compile ui
echo "[ui built ...]"
