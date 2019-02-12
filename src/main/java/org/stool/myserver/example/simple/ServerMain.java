package org.stool.myserver.example.simple;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.net.Buffer;

import java.nio.charset.Charset;

public class ServerMain {


    private static Logger log = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {

        HttpServer server = HttpServer.server();


        server.requestHandler(request -> {

            String param = request.getParam("key");
            log.info("请求参数：" + param);
            String contentType = request.getHeader(HttpHeaderNames.CONTENT_TYPE);
            log.info("请求头contentType：" + contentType);

            Buffer totalBuffer = Buffer.buffer();
            request.handler(buffer -> {
                totalBuffer.appendBuffer(buffer);
            });
            request.endHandler(v -> {
                String requestContent = totalBuffer.getByteBuf().toString(Charset.forName("utf-8"));
                log.info("客户端说：" + requestContent);
//                request.response().headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                request.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, 8);
//                request.response().end("{\"response\": \"Hello World!\"}");
                request.response().write("test");
                request.response().end("test");
            });

        }).listen(8082);

        server.start();

    }


}
