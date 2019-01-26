package org.stool.myserver.core.http;

import io.netty.handler.codec.http.HttpHeaders;
import org.stool.myserver.core.*;
import org.stool.myserver.core.http.impl.HttpClientOptions;
import org.stool.myserver.core.http.impl.HttpClientStream;

import java.util.function.Function;

public interface HttpClient {

    HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, HttpHeaders headers);

    HttpClientRequest request(HttpMethod method, String host, int port, String requestURI, HttpHeaders headers, Handler<AsyncResult<HttpClientResponse>> responseHandler);

    HttpClient connectionHandler(Handler<HttpConnection> handler);

    HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> handler);

    Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler();

    void close();

    EntryPoint getEntryPoint();

    HttpClientOptions getOptions();

    Handler<HttpConnection> connectionHandler();

    void getConnectionForRequest(Context ctx,
                                 String peerHost,
                                 int port,
                                 String host,
                                 Handler<AsyncResult<HttpClientStream>> handler);

}
