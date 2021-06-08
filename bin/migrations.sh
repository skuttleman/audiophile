#!/usr/bin/env sh

echo "[running migrations ${@} ...]"
clj -A:dev -m com.ben-allred.audiophile.backend.dev.migrations "${@}"
echo "[... migrations completed]"
