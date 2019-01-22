package org.stool.myserver.core.impl;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory {

    private static final Object FOO = new Object();

    private static Map<MyThread, Object> weakMap = new WeakHashMap<>();

    private static synchronized void addToMap(MyThread thread) {
        weakMap.put(thread, FOO);
    }


    public static synchronized void unsetContext(ContextImpl ctx) {
        for (MyThread thread: weakMap.keySet()) {
            if (thread.getContext() == ctx) {
                thread.setContext(null);
            }
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        MyThread myThread = new MyThread(r);
        addToMap(myThread);
        return myThread;
    }
}
