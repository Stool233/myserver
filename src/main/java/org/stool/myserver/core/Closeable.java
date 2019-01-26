package org.stool.myserver.core;

public interface Closeable {
    void close(Handler<AsyncResult<Void>> completionHandler);
}
