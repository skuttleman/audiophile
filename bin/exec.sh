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

function clean_ui() {
  rm -rf .shadow-cljs resources/public/css resources/public/js
}

function clean() {
  clean_ui
  rm -rf .cpcache classes target/audiophile.jar
}

function purge_ui() {
  rm -rf node_modules
}

function purge() {
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

    set -x
    case "${PROFILE}" in
      jar)
        clean
        build

        echo "running with profile: ${PROFILE}"
        LOG_LEVEL=info ENV=production SERVICES="api auth jobs ui" foreman start
        ;;
      single)
        echo "running with profile: ${PROFILE}"
        WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-single
        ;;
      split)
        echo "running with profile: ${PROFILE}"
        WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-split
        ;;
      multi)
        echo "running with profile: ${PROFILE}"
        WS_RECONNECT_MS=1000 LOG_LEVEL=debug ENV=development foreman start --procfile Procfile-multi
        ;;

      *)
        echo "unknown profile: ${PROFILE}: must be jar|single|split|multi"
        exit 1
        ;;
    esac
}

function test_ui() {
  clj -A:cljs-dev:test:shadow-cljs -Sthreads 1 compile test && \
    clj -A:dev -Sthreads 1 -m com.ben-allred.audiophile.test.browser-runner
}

function test() {
    clj -A:dev:test -Sthreads 1 -M:test && \
      test_ui
}

function help() {
    echo "Help"
    echo ""
    echo "  build   - build an uberjar"
    echo "  clean   - removes cached artifacts"
    echo "  deploy  - deploy the uberjar (use build first)"
    echo "  migrate - run db migration scripts"
    echo "  purge   - remove all locally cached files"
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
  clean)
    clean $@
    ;;
  deploy)
    deploy $@
    ;;
  migrate)
    migrate $@
    ;;
  purge)
    purge $@
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
