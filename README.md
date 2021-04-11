# Audiophile

This is a pet project to make a tool that helps audio engineers and musicians remotely collaborate and iterate on mixes and masters.

## Development

### Dependencies

- Install [NodeJs](https://nodejs.org/en/download/package-manager/)
- Install [JDK](https://docs.oracle.com/en/java/javase/16/install/overview-jdk-installation.html#GUID-8677A77F-231A-40F7-98B9-1FD0B48C346A)
- Install [Clojure runtime](https://clojure.org/guides/getting_started)
- Install [foreman](https://www.npmjs.com/package/foreman)

### Run application in dev mode

```bash
$ git clone git@github.com:skuttleman/audiophile.git
$ cd audiophile
$ npm install
$ npm start
```

You can override which ports the servers listen on.

```bash
$ SERVER_PORT=3000 NREPL_PORT=7000 UI_NREPL_PORT=7100 npm start
```

Visit `http://localhost:{SERVER_PORT:-3000}` in your browser to use app.

api nREPL listens at `localhost {NREPL_PORT:-7000}` (can be overridden with NREPL_PORT env var)
ui nREPL listens at `localhost {UI_NREPL_PORT:-7100}` (can be overridden with UI_NREPL_PORT env var)
