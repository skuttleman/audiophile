#!/usr/bin/env sh

clj -A:dev:test -Sthreads 1 -M:test && \
  clj -A:cljs-dev:test:shadow-cljs -Sthreads 1 compile test && \
  clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.test.browser-runner
