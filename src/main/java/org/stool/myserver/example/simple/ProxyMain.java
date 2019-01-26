package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpClient;

public class ProxyMain {

    private static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpClient client = entryPoint.createHttpClient();

    }
}
