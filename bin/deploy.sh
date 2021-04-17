#!/usr/bin/env sh

echo "[deploying ...]"
heroku deploy:jar target/audiophile.jar --app skuttleman-audiophile
echo "[... deployed]"
