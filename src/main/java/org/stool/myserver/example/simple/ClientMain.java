package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpClient;
import org.stool.myserver.core.http.HttpClientRequest;
import org.stool.myserver.core.http.HttpClientResponse;
import org.stool.myserver.core.http.HttpMethod;

import java.nio.charset.Charset;

public class ClientMain {

    public static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpClient httpClient = entryPoint.createHttpClient();
        HttpClientRequest request = httpClient.request(HttpMethod.GET, "127.0.0.1", 8080, "/", null, ar -> {
            if (ar.succeeded()) {
                HttpClientResponse resp = ar.result();
                System.out.println("Got response " + resp.statusCode());
                resp.bodyHandler(body -> System.out.println("Got data " + body.getByteBuf().toString(Charset.forName("utf-8"))));
            }
        });

        request.end();
    }

}
