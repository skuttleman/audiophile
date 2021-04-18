#!/usr/bin/env sh

clj -A:dev:test -M:test && clj -A:cljs-dev:test:shadow-cljs compile test && clj -A:dev -m test.browser-runner
