## Running application

Simply run ```sbt run``` in your console.

Use your favourite web socket client.
Author tested code with next firefox browser extension:

* [Simple WebSocket Client](https://addons.mozilla.org/en-US/firefox/addon/simple-websocket-client/)
* [Browser WebSocket Client ](https://addons.mozilla.org/en-US/firefox/addon/browser-websocket-client/)

## Built With

* [Akka-http](https://doc.akka.io/docs/akka-http/current/) - For http server impl
* [Akka-stream](https://doc.akka.io/docs/akka/2.5.16/stream/index.html) - For stream ws flow
* [Akka-akka-cluster-tools](https://doc.akka.io/docs/akka/2.5/cluster-client.html) - Publish and subscribe on events
* [Circe](https://github.com/circe/circe) - For json processing

## Authors

* **Dmitry Danilenko**
