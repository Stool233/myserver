package org.stool.myserver.core.proxy;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.*;
import org.stool.myserver.core.net.SocketAddress;
import org.stool.myserver.core.net.impl.SocketAddressImpl;
import org.stool.myserver.route.RoutingContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyHandler implements Handler<RoutingContext>{

    private final HttpClient httpClient;

    private List<SocketAddress> remoteServers;
    private int remoteServersIndex;

    public static ProxyHandler create(EntryPoint entryPoint) {
        return new ProxyHandler(entryPoint);
    }

    public ProxyHandler(EntryPoint entryPoint) {
        remoteServers = new ArrayList<>();
        remoteServersIndex = 0;
        httpClient = entryPoint.createHttpClient();
    }

    public ProxyHandler addRemoteServer(String host, int port) {
        remoteServers.add(new SocketAddressImpl(host, port));
        return this;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        SocketAddress socketAddress =  nextServer();
        if (socketAddress == null) {
            return ;
        }

        handleProxy(routingContext.request(), routingContext.response(), socketAddress.host(), socketAddress.port());
    }

    private SocketAddress nextServer() {
        if (remoteServers.size() == 0) {
            return null;
        }

        SocketAddress socketAddress = remoteServers.get(remoteServersIndex);

        remoteServersIndex++;

        if (remoteServersIndex == remoteServers.size()) {
            remoteServersIndex = 0;
        }

        return socketAddress;
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
