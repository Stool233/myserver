package org.stool.myserver.example.simple;

import org.stool.myserver.core.EntryPoint;

public class SendFileMain {
    public static void main(String[] args) {

        String dir = "C:/Users/Administrator/Downloads/myserver/src/main/resources/";

        EntryPoint entryPoint = EntryPoint.entryPoint();
        entryPoint.createHttpServer().requestHandler(req -> {
            String filename = null;
            if (req.path().equals("/")) {
                filename = dir + "index.html";
            } else if (req.path().equals("/page1.html")) {
                filename = dir + "page1.html";
            } else if (req.path().equals("/page2.html")) {
                filename = dir + "page2.html";
            } else {
                req.response().setStatusCode(404).end();
            }
            if (filename != null) {
                req.response().sendFile(filename);
            }
        }).listen(8080);
    }
}
