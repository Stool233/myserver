package org.stool.myserver.core.http;

import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;
import org.stool.myserver.core.net.SocketAddress;

public interface HttpConnection {

    default int getWindowSize() {
        return -1;
    }

    HttpConnection closeHandler(Handler<Void> handler);

    HttpConnection exceptionHandler(Handler<Throwable> handler);

    void close();

    SocketAddress remoteAddress();

    SocketAddress localAddress();

    Context getContext();

}
