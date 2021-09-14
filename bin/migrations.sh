#!/usr/bin/env sh

echo "[running migrations ${@} ...]"
clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.backend.dev.migrations "${@}"
echo "[... migrations completed]"
