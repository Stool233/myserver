package org.stool.myserver.core.impl;

import org.stool.myserver.core.Context;

public class MyThread extends Thread{

    private Context context;

    public MyThread(Runnable target) {
        super(target);
    }

    public MyThread(Runnable target, String name) {
        super(target, name);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}
