#!/usr/bin/env sh

PROFILE="${1:-prod}"

echo "running with profile: ${PROFILE}"

case "${PROFILE}" in
prod)
  $(dirname ${BASH_SOURCE[0]})/build-ui.sh
  clj -m com.ben-allred.audiophile.api.server
  ;;
dev)
  rm -rf node_modules
  npm install
  LOG_LEVEL=debug ENV=development nf --procfile Procfile-dev start
  ;;
*)
  echo "unknown profile: ${PROFILE}"
  exit 1
  ;;
esac
