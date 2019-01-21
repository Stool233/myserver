package org.stool.myserver.core.http;

public interface HttpClientRequest {
    void handleException(Throwable cause);

    void handleResponse(HttpClientResponse response);

    void handleDrained();

}
