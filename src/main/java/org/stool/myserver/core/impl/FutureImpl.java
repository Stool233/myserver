package org.stool.myserver.core.impl;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;

import java.util.function.Function;

public class FutureImpl<T> implements Future<T> {

    private boolean failed;
    private boolean succeeded;
    private Handler<AsyncResult<T>> handler;
    private T result;
    private Throwable throwable;

    public FutureImpl() {

    }

    @Override
    public void complete() {
        if (!tryComplete()) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public void complete(T result) {
        if (!tryComplete(result)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public boolean isComplete() {
        return failed || succeeded;
    }

    @Override
    public void fail(Throwable cause) {
        if (!tryFail(cause)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public void fail(String failureMessage) {
        if (!tryFail(failureMessage)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public boolean tryComplete() {
        return tryComplete(null);
    }

    @Override
    public boolean tryComplete(T result) {
        Handler<AsyncResult<T>> h;
        synchronized (this) {
            if (succeeded || failed) {
                return false;
            }
            this.result = result;
            succeeded = true;
            h = handler;
            handler = null;
        }
        if (h != null) {
            h.handle(this);
        }
        return true;
    }

    @Override
    public boolean tryFail(Throwable cause) {
        Handler<AsyncResult<T>> h;
        synchronized (this) {
            if (succeeded || failed) {
                return false;
            }
            this.throwable = cause != null ? cause : new Exception();
            failed = true;
            h = handler;
            handler = null;
        }
        if (h != null) {
            h.handle(this);
        }
        return true;
    }

    @Override
    public boolean tryFail(String failureMessage) {
        return tryFail(new Exception(failureMessage));
    }

    @Override
    public Future<T> setHandler(Handler<AsyncResult<T>> handler) {
        boolean callHandler;
        synchronized (this) {
            callHandler = isComplete();
            if (!callHandler) {
                this.handler = handler;
            }
        }
        if (callHandler) {
            handler.handle(this);
        }
        return this;
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public boolean failed() {
        return failed;
    }

    @Override
    public Throwable cause() {
        return throwable;
    }

    @Override
    public void handle(AsyncResult<T> asyncResult) {
        if (asyncResult.succeeded()) {
            complete(asyncResult.result());
        } else {
            fail(asyncResult.cause());
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (succeeded) {
                return "Future{result=" + result + "}";
            }
            if (failed) {
                return "Future{cause=" + throwable.getMessage() + "}";
            }
            return "Future{unresolved}";
        }
    }

    @Override
    public Handler<AsyncResult<T>> getHandler() {
        return handler;
    }
}
