#!/usr/bin/env sh

TYPE="${1:-clj}"

case "${TYPE}" in
clj)
  echo "running with type: ${TYPE}"
  clj -C:dev:test -Sthreads 1 -e "(nrepl)" -r
  ;;
cljs)
  echo "running with type: ${TYPE}"
  clj -A:cljs-dev:shadow-cljs -Sthreads 1 browser-repl | grep --color=never -v DEBUG
  ;;
*)
  echo "unknown type: ${TYPE}"
  exit 1
  ;;
esac
