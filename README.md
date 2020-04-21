# 基于Netty的异步HTTP Server

参考了Vert.x

### 启动server例子
```java
EntryPoint entryPoint = EntryPoint.entryPoint();
HttpServer httpServer = entryPoint.createHttpServer();

RouteHandler routeHandler = RouteHandler.create(entryPoint);

routeHandler.route(HttpMethod.GET, "/a/*").handler(routingContext -> {
      routingContext.response().end("a");
});

routeHandler.route(HttpMethod.GET, "/b/*").handler(routingContext -> {
      routingContext.response().end("b");
});

httpServer.requestHandler(routeHandler).listen(8080);
```
