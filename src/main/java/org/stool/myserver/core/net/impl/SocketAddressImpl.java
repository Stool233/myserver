package org.stool.myserver.core.net.impl;

import org.stool.myserver.core.net.SocketAddress;

import java.net.InetSocketAddress;

public class SocketAddressImpl implements SocketAddress {

    private final String hostAddress;
    private final String path;
    private final int port;


    public SocketAddressImpl(InetSocketAddress addr) {
        this(addr.getAddress().getHostAddress(), addr.getPort());
    }

    public SocketAddressImpl(String host, int port) {
        this.port = port;
        this.hostAddress = host;
        this.path = null;
    }

    public SocketAddressImpl(String path) {
        this.port = -1;
        this.hostAddress = null;
        this.path = path;
    }

    @Override
    public String host() {
        return hostAddress;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String path() {
        return path;
    }
}
