package org.stool.myserver.core;

import org.stool.myserver.core.EntryPoint;

public interface Server {

    void init(EntryPoint entryPoint);

    void start();

    void stop();
}
