#!/usr/bin/env sh

set -e

function build_ui() {
  echo "[building ui ...]"
  rm -rf node_modules
  npm install
  rm -rf resources/public/css
  sass --style=compressed src/scss/main.scss resources/public/css/main.css
  rm -rf resources/public/js
  clj -A:shadow-cljs -Sthreads 1 compile ui
  echo "[ui built ...]"
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
    PROFILE="${1:-dev}"

    case "${PROFILE}" in
      prod)
        build_ui

        echo "running with profile: ${PROFILE}"
        clj -Sthreads 1 -m com.ben-allred.audiophile.backend.core api auth jobs ui
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
}

function test() {
    clj -A:dev:test -Sthreads 1 -M:test && \
      clj -A:cljs-dev:test:shadow-cljs -Sthreads 1 compile test && \
      clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.test.browser-runner
}

function help() {
    echo "Help"
    echo ""
    echo "  build   - build an uberjar"
    echo "  deploy  - deploy the uberjar (use build first)"
    echo "  migrate - run db migration scripts"
    echo "  run     - run the application"
    echo "  test    - run tests"
    echo "  help    - display this information"
}

FUNCTION=${1}

if [ ${1+x} ]; then
  shift
fi

case "${FUNCTION}" in
  build)
    build $@
    ;;
  deploy)
    deploy $@
    ;;
  migrate)
    migrate $@
    ;;
  run)
    run $@
    ;;
  test)
    test $@
    ;;
  help)
    help
    ;;
  *)
    echo "unknown function ${FUNCTION}"
    help
    ;;
esac
