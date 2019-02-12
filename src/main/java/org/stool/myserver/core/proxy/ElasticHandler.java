package org.stool.myserver.core.proxy;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.*;
import org.stool.myserver.route.RoutingContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticHandler implements Handler<HttpServerRequest> {

    private EntryPoint entryPoint;
    private HttpClient httpClient;
    private List<HttpServer> httpServers;
    private int serverPoolSize;
    private Handler<HttpServerRequest> requestHandler;
    private int serverIndex;

    public static ElasticHandler create(EntryPoint entryPoint, int serverPoolSize, Handler<HttpServerRequest> requestHandler) {
        return new ElasticHandler(entryPoint, serverPoolSize, requestHandler);
    }

    public ElasticHandler(EntryPoint entryPoint, int serverPoolSize, Handler<HttpServerRequest> requestHandler) {
        this.entryPoint = entryPoint;
        this.serverPoolSize = serverPoolSize;
        this.requestHandler = requestHandler;
        init();
    }

    private void init() {
        httpServers = new ArrayList<>();
        List<Integer> ports = getNewPorts(serverPoolSize);
        for (int i = 0; i < serverPoolSize; i++) {
            HttpServer httpServer = entryPoint.createHttpServer().requestHandler(requestHandler).listen(ports.get(i));
            httpServers.add(httpServer);
        }
        httpClient = entryPoint.createHttpClient();
    }

    private List<Integer> getNewPorts(int nums) {
        List<ServerSocket> serverSockets = new ArrayList<>();
        try {
            for (int i = 0; i < nums; i++) {
                ServerSocket serverSocket = new ServerSocket(0);
                serverSockets.add(serverSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Integer> ports = serverSockets.stream().map(ServerSocket::getLocalPort).collect(Collectors.toList());
        try {
            for (ServerSocket serverSocket : serverSockets) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ports;
    }

    private HttpServer nextServer() {
        HttpServer httpServer = httpServers.get(serverIndex);
        serverIndex++;
        if (serverIndex == httpServers.size()) {
            serverIndex = 0;
        }
        return httpServer;
    }

    private void handleProxy(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, String host, int port) {
        HttpClientRequest httpClientRequest = httpClient.request(httpServerRequest.method(),
                host, port, httpServerRequest.uri(), httpServerRequest.headers(), ar -> {
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
    }

    private static void setHeaders(HttpHeaders headersFrom, HttpHeaders headersTo) {

        if (headersFrom == null || headersTo == null) {
            return;
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


    @Override
    public void handle(HttpServerRequest request) {
        HttpServer httpServer = nextServer();
        String host;
        int index;
        if ((index = request.host().lastIndexOf(":")) != -1) {
            host = request.host().substring(0, index);
        } else {
            host = request.host();
        }
        handleProxy(request, request.response(), host, httpServer.actualPort());
    }
}
