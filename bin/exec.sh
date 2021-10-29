#!/usr/bin/env sh

set -e

function install() {
  cp bin/pre-commit.sh .git/hooks/pre-commit
  npm install
  clj -X:deps prep
}

function seed() {
  psql audiophile -f $PWD/dev/resources/db/seed.sql
}

function build_ui() {
  echo "[building ui ...]"
  npm install
  rm -rf resources/public/css
  sass --style=compressed src/scss/main.scss resources/public/css/main.css
  rm -rf resources/public/js
  clj -A:shadow-cljs -Sthreads 1 -m shadow.cljs.devtools.cli compile ui
  echo "[... ui built]"
}

function clean_ui() {
  rm -rf .shadow-cljs resources/public/css resources/public/js node_modules
}

function clean() {
  echo "[cleaning caches ...]"
  clean_ui
  rm -rf .cpcache classes target/audiophile.jar
  echo "[caches clean ..."
}

function purge_ui() {
  rm -rf node_modules
}

function purge() {
  echo "you don't actually want to purge everything"
  exit 1

  purge_ui
  rm -rf .m2
  clean
}

function build() {
  build_ui

  echo "[building uberjar ...]"
  rm -rf classes
  mkdir classes
  rm -f target/audiophile.jar
  clj -Sthreads 1 -e "(compile 'com.ben-allred.audiophile.backend.core)"
  LOG_LEVEL=warn clj -Sthreads 1 -A:uberjar -m uberdeps.uberjar --level warn --target target/audiophile.jar --main-class com.ben_allred.audiophile.backend.core
  echo "[... uberjar built]"
}

function dockerize() {
  build

  echo "[building docker containers ...]"
  docker build -t audiophile -f Dockerfile .
  docker tag audiophile skuttleman/audiophile:latest
  docker push skuttleman/audiophile:latest

  docker build -t audiophile-dev -f Dockerfile-dev .
  docker tag audiophile-dev skuttleman/audiophile:dev
  docker push skuttleman/audiophile:dev
  echo "[... docker containers built]"
}

function deploy() {
  echo "[deploying ...]"
  heroku deploy:jar target/audiophile.jar --app skuttleman-audiophile
  echo "[... deployed]"
}

function migrate() {
  echo "[running migrations ${@} ...]"
  clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.backend.dev.migrations "${@}"
  echo "[... migrations completed]"
}

function run() {
  PROFILE="${1:-single}"
  rm -rf classes
  npm install > /dev/null 2>&1

  set -x
  case "${PROFILE}" in
    jar)
      clean
      build

      echo "running with profile: ${PROFILE}"
      LOG_LEVEL="${LOG_LEVEL:-info}" ENV=production SERVICES="api auth jobs ui" foreman start
      ;;
    single)
      echo "running with profile: ${PROFILE}"
      WS_RECONNECT_MS=1000 LOG_LEVEL="${LOG_LEVEL:-debug}" ENV=development foreman start --procfile Procfile-single
      ;;
    split)
      echo "running with profile: ${PROFILE}"
      WS_RECONNECT_MS=1000 LOG_LEVEL="${LOG_LEVEL:-debug}" ENV=development foreman start --procfile Procfile-split
      ;;
    multi)
      echo "running with profile: ${PROFILE}"
      WS_RECONNECT_MS=1000 LOG_LEVEL="${LOG_LEVEL:-debug}" ENV=development foreman start --procfile Procfile-multi
      ;;

    *)
      echo "unknown profile: ${PROFILE}: must be jar|single|split|multi"
      exit 1
      ;;
  esac
}

function test_ui() {
  clj -A:cljs-dev:test:shadow-cljs -Sthreads 1 -m shadow.cljs.devtools.cli compile test && \
    clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.test.browser-runner
}

function test() {
  clj -A:dev:test -Sthreads 1 -m kaocha.runner && test_ui
}

function wipe() {
  EXT=".${1}"
  if [ -z "$1" ]; then
    EXT=""
  fi
  echo "deleting artifacts"
  /bin/rm -f target/artifacts/*
  echo "artifacts deleted"

  echo "resetting postgres data"
  psql audiophile -c " \
    BEGIN; \
    TRUNCATE projects CASCADE; \
    TRUNCATE events CASCADE; \
    TRUNCATE users CASCADE; \
    TRUNCATE teams CASCADE; \
    DELETE FROM artifacts; \
    COMMIT"
  echo "postgres data reset"

  echo "deleting rabbitmq queues"
  for QUEUE in $(curl -s http://guest:guest@localhost:15672/api/queues | \
                 jq -r ".[] | .name | select(test(\"(audiophile${EXT})\"))"); do
    echo "deleting ${QUEUE}"
    rabbitmqadmin delete queue name=$QUEUE
  done
  echo "all queues deleted from rabbitmq"

  echo "deleting rabbitmq exchanges"
  for EXCHANGE in $(curl -s http://guest:guest@localhost:15672/api/exchanges | \
                    jq -r ".[] | .name | select(test(\"(audiophile${EXT})\"))"); do
    echo "deleting ${EXCHANGE}"
    rabbitmqadmin delete exchange name=$EXCHANGE
  done
  echo "all exchanges deleted from rabbitmq"
}

FUNCTION=${1}

if [ ${1+x} ]; then
  shift
fi

if [[ $(type -t $FUNCTION) == function ]]; then
  $FUNCTION $@
  exit 0
fi

1>&2 echo "unknown function: '${FUNCTION}'"
exit 1
