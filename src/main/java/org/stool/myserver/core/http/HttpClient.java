package org.stool.myserver.core.http;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.impl.HttpClientOptions;

import java.util.function.Function;

public interface HttpClient {
    HttpClientRequest request(HttpMethod method, String host, int port, String requestURI);

    HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, Handler<AsyncResult<HttpClientResponse>> responseHandler);

    HttpClient connectionHandler(Handler<HttpConnection> handler);

    HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> handler);

    Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler();

    void close();

    EntryPoint getEntryPoint();

    HttpClientOptions getOptions();

}
