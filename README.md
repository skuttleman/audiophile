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
- Install [Make](https://askubuntu.com/questions/161104/how-do-i-install-make)

### Install

```bash
$ git clone git@github.com:skuttleman/audiophile.git
$ cd audiophile
$ make install
```

### Run application in dev mode

```bash
$ make run # runs the entire server side api in a single process
$ make run-split # runs the server side api as separate microservices
$ make run-multi # runs multiple instances of each microservice
$ make run-jar # builds static assets and run a pre-compiled jar without any dev-only implementations
```

Visit `http://localhost:{UI_PORT:-8080}` in your browser to use app.

## Tests

### Dependencies

- Install [firefox](https://www.mozilla.org/en-US/firefox/mac/)
- Install [gecko](https://www.kenst.com/2016/12/installing-marionette-firefoxdriver-on-mac-osx/)

### Run tests

```bash
$ make tests
```

## ERD

![ERD](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/skuttleman/audiophile/master/docs/diagrams/erd.puml)

## Sequence Diagram

![ERD](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/skuttleman/audiophile/master/docs/diagrams/files.puml)
