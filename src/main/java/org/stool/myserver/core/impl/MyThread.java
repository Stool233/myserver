package org.stool.myserver.core.impl;

import org.stool.myserver.core.Context;

public class MyThread extends Thread{

    private final boolean worker;
    private Context context;

    public MyThread(Runnable target) {
        super(target);
        this.worker = false;
    }

    public MyThread(Runnable target, String name) {
        super(target, name);
        this.worker = false;
    }

    public MyThread(Runnable target, boolean worker) {
        super(target);
        this.worker = worker;
    }

    public MyThread(Runnable target, String name, boolean worker) {
        super(target, name);
        this.worker = worker;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public boolean isWorker() {
        return worker;
    }
}
