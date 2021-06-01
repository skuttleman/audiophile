#!/usr/bin/env sh

PROFILE="${1:-prod}"

case "${PROFILE}" in
prod)
  echo "running with profile: ${PROFILE}"
  $(dirname ${BASH_SOURCE[0]})/build-ui.sh
  clj -m com.ben-allred.audiophile.api.core
  ;;
dev)
  echo "running with profile: ${PROFILE}"
  WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-dev
  ;;
dev-test)
  echo "running with profile: ${PROFILE}"
  WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-dev-test
  ;;
*)
  echo "unknown profile: ${PROFILE}"
  exit 1
  ;;
esac
