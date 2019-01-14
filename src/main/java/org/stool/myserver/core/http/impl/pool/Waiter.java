package org.stool.myserver.core.http.impl.pool;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Handler;

public class Waiter<C> {

    public final Handler<AsyncResult<C>> handler;

    public Waiter(Handler<AsyncResult<C>> handler) {
        this.handler = handler;
    }
}
