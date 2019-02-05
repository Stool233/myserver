package org.stool.myserver.core.route;

import org.stool.myserver.core.http.HttpServerRequest;
import org.stool.myserver.core.http.HttpServerResponse;

public interface RoutingContext {

    HttpServerRequest request();

    HttpServerResponse response();
}