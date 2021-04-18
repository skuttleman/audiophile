#!/usr/bin/env sh

TYPE="${1:-clj}"

case "${TYPE}" in
clj)
  echo "running with type: ${TYPE}"
  clj -C:dev:test -e "(nrepl)" -r
  ;;
cljs)
  echo "running with type: ${TYPE}"
  clj -A:cljs-dev:shadow-cljs browser-repl | grep --color=never -v DEBUG
  ;;
*)
  echo "unknown type: ${TYPE}"
  exit 1
  ;;
esac
