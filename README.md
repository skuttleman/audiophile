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
$ bin/exec.sh run single # runs the entire server side api in a single process
$ bin/exec.sh run split # runs the server side api as separate microservices
$ bin/exec.sh run multi # runs multiple instances of each microservice
$ bin/exec.sh run jar # builds static assets and run a pre-compiled jar without any dev-only implementations
```

Visit `http://localhost:{UI_PORT:-8080}` in your browser to use app.

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

## Sequence Diagram

![ERD](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/skuttleman/audiophile/master/resources/doc/diagrams/erd.puml)
