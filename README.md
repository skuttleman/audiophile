# Audiophile

This is a pet project to make a tool that helps audio engineers and musicians remotely collaborate and iterate on mixes and masters.

## Development

### Dependencies

- Install [NodeJs](https://nodejs.org/en/download/package-manager/)
- Install [JDK](https://docs.oracle.com/en/java/javase/16/install/overview-jdk-installation.html#GUID-8677A77F-231A-40F7-98B9-1FD0B48C346A)
- Install [Clojure runtime](https://clojure.org/guides/getting_started)
- Install [Sass](https://sass-lang.com/install)
- Install [Foreman](http://blog.daviddollar.org/2011/05/06/introducing-foreman.html)
- Install [Graphviz](https://graphviz.org/download/)

### Install

```bash
$ git clone git@github.com:skuttleman/audiophile.git
$ cd audiophile
$ cp bin/pre-commit.sh .git/hooks/pre-commit
$ npm install
```

### Run application in dev mode

```bash
$ bin/exec.sh run dev
```

You can override which ports the servers listen on.

```bash
$ API_PORT=3000 \
  AUTH_PORT=3100 \
  EVENT_PORT=3200 \
  UI_PORT=8080 \
  API_NREPL_PORT=7000 \
  AUTH_NREPL_PORT=7100 \
  EVENT_NREPL_PORT=7200 \
  UI_NREPL_PORT=8180 \
  CLJS_NREPL_PORT=8280 \
  bin/exec.sh run dev
```

Visit `http://localhost:{UI_PORT:-8080}` in your browser to use app.

api nREPL listens at `localhost {NREPL_PORT:-7000}` (can be overridden with API_NREPL_PORT env var)
auth nREPL listens at `localhost {NREPL_PORT:-7100}` (can be overridden with AUTH_NREPL_PORT env var)
event nREPL listens at `localhost {NREPL_PORT:-7100}` (can be overridden with EVENT_NREPL_PORT env var)
ui nREPL listens at `localhost {UI_NREPL_PORT:-8180}` (can be overridden with UI_NREPL_PORT env var)
cljs compiler nREPL listens at `localhost {CLJS_NREPL_PORT:-8280}` (can be overridden with CLJS_NREPL_PORT env var)

## Tests

### Dependencies
- Install [firefox](https://www.mozilla.org/en-US/firefox/mac/)
- Install [gecko](https://www.kenst.com/2016/12/installing-marionette-firefoxdriver-on-mac-osx/)

### Run tests

```bash
$ bin/exec.sh test
```

## ERD

![ERD](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/skuttleman/audiophile/master/resources/db/erd.puml)
