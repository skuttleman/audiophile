#!/usr/bin/env sh

clj -A:test -M:test && clj -A:test:cljs-test:shadow-cljs compile test
