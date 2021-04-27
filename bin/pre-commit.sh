#!/usr/bin/env sh

echo "Running tests before committing…"
bin/test.sh
STATUS="${?}"

echo ''

if [ "${STATUS}" == "0" ]; then
    echo "[32mAll tests passed. committing…[0m"
else
    echo "[31mTests failed. commit aborted.[0m"
fi

exit $STATUS
