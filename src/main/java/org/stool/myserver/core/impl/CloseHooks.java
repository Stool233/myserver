package org.stool.myserver.core.impl;

import org.slf4j.Logger;
import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Closeable;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class CloseHooks {

    private final Logger log;
    private boolean closeHooksRun;
    private Set<Closeable> closeHooks;

    CloseHooks(Logger log) {
        this.log = log;
    }

    /**
     * Add a close hook, notified when the {@link #run(Handler)} method is called.
     *
     * @param hook the hook to add
     */
    synchronized void add(Closeable hook) {
        if (closeHooks == null) {
            // Has to be concurrent as can be removed from non context thread
            closeHooks = new HashSet<>();
        }
        closeHooks.add(hook);
    }

    /**
     * Remove an existing hook.
     *
     * @param hook the hook to remove
     */
    synchronized void remove(Closeable hook) {
        if (closeHooks != null) {
            closeHooks.remove(hook);
        }
    }

    /**
     * Run the close hooks.
     *
     * @param completionHandler called when all hooks have beene executed
     */
    void run(Handler<AsyncResult<Void>> completionHandler) {
        Set<Closeable> copy = null;
        synchronized (this) {
            if (closeHooksRun) {
                // Sanity check
                throw new IllegalStateException("Close hooks already run");
            }
            closeHooksRun = true;
            if (closeHooks != null && !closeHooks.isEmpty()) {
                // Must copy before looping as can be removed during loop otherwise
                copy = new HashSet<>(closeHooks);
            }
        }
        if (copy != null && !copy.isEmpty()) {
            int num = copy.size();
            if (num != 0) {
                AtomicInteger count = new AtomicInteger();
                AtomicBoolean failed = new AtomicBoolean();
                for (Closeable hook : copy) {
                    Future<Void> a = Future.future();
                    a.setHandler(ar -> {
                        if (ar.failed()) {
                            if (failed.compareAndSet(false, true)) {
                                // Only report one failure
                                completionHandler.handle(Future.failedFuture(ar.cause()));
                            }
                        } else {
                            if (count.incrementAndGet() == num) {
                                // closeHooksRun = true;
                                completionHandler.handle(Future.succeededFuture());
                            }
                        }
                    });
                    try {
                        hook.close(a);
                    } catch (Throwable t) {
                        log.warn("Failed to run close hooks", t);
                        a.tryFail(t);
                    }
                }
            } else {
                completionHandler.handle(Future.succeededFuture());
            }
        } else {
            completionHandler.handle(Future.succeededFuture());
        }
    }

}

