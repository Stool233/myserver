package org.stool.myserver.core.impl;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        return new MyThread(r);
    }
}
