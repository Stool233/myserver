package org.stool.myserver.example.simple;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.*;

public class ProxyMain {

    public static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();

        HttpServer httpServer = entryPoint.createHttpServer();
        HttpClient httpClient = entryPoint.createHttpClient();

        httpServer.requestHandler(httpServerRequest -> {

            HttpServerResponse httpServerResponse = httpServerRequest.response();

            HttpClientRequest httpClientRequest = httpClient.request(httpServerRequest.method(),
                    "127.0.0.1", 8080, httpServerRequest.uri(), httpServerRequest.headers(), ar -> {
                        if (ar.succeeded()) {
                            HttpClientResponse httpClientResponse = ar.result();
                            httpServerResponse.setStatusCode(httpClientResponse.statusCode());
                            setHeaders(httpClientResponse.headers(), httpServerResponse.headers());
                            httpClientResponse.handler(data -> {
                                httpServerResponse.write(data);
                            });
                            httpClientResponse.endHandler(v -> {
                                httpServerResponse.end();
                            });
                        }
                    });
            setHeaders(httpServerRequest.headers(), httpClientRequest.headers());
            httpServerRequest.handler(data -> {
                httpClientRequest.write(data);
            });
            httpServerRequest.endHandler(v -> {
                httpClientRequest.end();
            });

        }).listen(8082);

    }

    private static void setHeaders(HttpHeaders headersFrom, HttpHeaders headersTo) {

        if (headersFrom == null || headersTo == null) {
            return ;
        }

        for (String key : headersFrom.names()) {
            if (key == null) {
                continue;
            }

            if (headersFrom.get(key) != null) {
                headersTo.set(key, headersFrom.get(key));
            }

        }
    }

}
