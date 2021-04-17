#!/usr/bin/env sh

clj -A:dev -m com.ben-allred.audiophile.api.dev.migrations "$@"
