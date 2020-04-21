# 基于Netty的异步HTTP Server

参考了Vert.x

启动server例子
```
    EntryPoint.entryPoint()
                .createHttpServer()
                .requestHandler(request -> {
                    request.response().end("Hello World");
                }).listen(8085);
```
