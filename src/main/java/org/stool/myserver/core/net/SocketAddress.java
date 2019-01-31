package org.stool.myserver.core.net;

import org.stool.myserver.core.net.impl.SocketAddressImpl;

public interface SocketAddress {

    static SocketAddress inetSocketAddress(String host, int port) {
        return new SocketAddressImpl(host, port);
    }
    String host();

    int port();

    String path();
}
