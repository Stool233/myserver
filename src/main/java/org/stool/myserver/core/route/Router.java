package org.stool.myserver.core.route;

import org.stool.myserver.core.Context;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServerRequest;

public interface Router extends Handler<HttpServerRequest>{

    Route route();

    Route route(HttpMethod method, String path);

    Route route(String path);

    EntryPoint entryPoint();

}
