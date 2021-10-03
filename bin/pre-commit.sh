#!/usr/bin/env sh

function references() {
  if [ -d $1 ]; then
    if [[ $(ag "\.${2}\." "${1}") ]];then
      echo "[31mfound invalid references to ${2} in ${1}.[0m"
      exit 1
    fi
  fi
}

function reference_group() {
  references src/clj/com/ben_allred/audiophile/backend/$1 $2
  references src/cljc/com/ben_allred/audiophile/common/$1 $2
  references src/cljs/com/ben_allred/audiophile/ui/$1 $2
  references test/clj/com/ben_allred/audiophile/backend/$1 $2
  references test/cljc/com/ben_allred/audiophile/common/$1 $2
  references test/cljs/com/ben_allred/audiophile/ui/$1 $2
}

echo 'checking inverted dependencies'
reference_group api infrastructure
reference_group domain infrastructure
reference_group domain api
reference_group core infrastructure
reference_group core api
reference_group core domain
echo "[32mdependencies check passed[0m"

echo "Running tests before committing…"
make test
STATUS="${?}"

echo ''

if [ "${STATUS}" == "0" ]; then
    echo "[32mAll tests passed. committing…[0m"
else
    echo "[31mTests failed. commit aborted.[0m"
fi

exit $STATUS
