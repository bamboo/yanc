# YANC

Yet another node/ClojureScript/WebSocket experiment.

## Usage

```
lein start
```

which is the same as:

```
lein npm install
lein resource
lein cljsbuild once
cd target/app
node server.js
```

Open two different browsers at [http://localhost:8080/](http://localhost:8080/) and have a blast talking to yourself.

There's also a client built with [Reagent](http://holmsand.github.io/reagent/) - a minimalistic interface between ClojureScript and [React](http://facebook.github.io/react/) - at [http://localhost:8080/react.html](http://localhost:8080/react.html). 

## Hacking

To get a node cider REPL going, use cider-jack-in and at the repl:

```
user> (require '[cljs.repl.node :as node])
nil
user> (node/run-node-nrepl)
Type `:cljs/quit` to stop the ClojureScript REPL
nil
```

## License

Copyright © 2014 Rodrigo B. de Oliveira

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
