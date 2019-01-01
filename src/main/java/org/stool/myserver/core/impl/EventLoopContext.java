package org.stool.myserver.core.impl;

import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.Handler;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class EventLoopContext extends ContextImpl {

    public EventLoopContext(EntryPoint owner) {
        super(owner);
    }

    @Override
    public void executeAsync(Handler<Void> task) {
        getEventLoop().execute(() -> task.handle(null));
    }

    @Override
    protected <T> void execute(T value, Handler<T> task) {
        executeTask(value, task);
    }
}
