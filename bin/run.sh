#!/usr/bin/env sh

PROFILE="${1:-prod}"

case "${PROFILE}" in
prod)
  echo "running with profile: ${PROFILE}"
  $(dirname ${BASH_SOURCE[0]})/build-ui.sh
  clj -Sthreads 1 -m com.ben-allred.audiophile.backend.core api auth event ui
  ;;
dev)
  echo "running with profile: ${PROFILE}"
  WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-dev
  ;;
*)
  echo "unknown profile: ${PROFILE}"
  exit 1
  ;;
esac
