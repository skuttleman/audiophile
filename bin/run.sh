#!/usr/bin/env sh

PROFILE="${1:-prod}"

case "${PROFILE}" in
prod)
  echo "running with profile: ${PROFILE}"
  $(dirname ${BASH_SOURCE[0]})/build-ui.sh
  clj -m com.ben-allred.audiophile.api.server
  ;;
dev)
  echo "running with profile: ${PROFILE}"
  LOG_LEVEL=debug ENV=development nf --env '' --procfile Procfile-dev start
  ;;
dev-test)
  echo "running with profile: ${PROFILE}"
  LOG_LEVEL=debug ENV=development nf --env '' --procfile Procfile-dev-test start
  ;;
*)
  echo "unknown profile: ${PROFILE}"
  exit 1
  ;;
esac
