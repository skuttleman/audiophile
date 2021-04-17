#!/usr/bin/env sh

PROFILE="${1:-prod}"

case "${PROFILE}" in
prod)
  echo "running with profile: prod"
  $(dirname ${BASH_SOURCE[0]})/build-ui.sh
  clj -m com.ben-allred.audiophile.api.server | grep -v com.zaxxer.hikari
  ;;
dev)
  echo "running with profile: dev"
  LOG_LEVEL=debug ENV=development nf --procfile Procfile-dev start
  ;;
*)
  echo "unknown profile: ${PROFILE}"
  exit 1
  ;;
esac
