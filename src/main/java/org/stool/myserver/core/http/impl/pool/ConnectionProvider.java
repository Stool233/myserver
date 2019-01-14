package org.stool.myserver.core.http.impl.pool;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Context;
import org.stool.myserver.core.Handler;

public interface ConnectionProvider<C> {

    void connect(ConnectionListener<C> listener, Context context, Handler<AsyncResult<ConnectResult<C>>> resultHandler);

    void close(C conn);
}
