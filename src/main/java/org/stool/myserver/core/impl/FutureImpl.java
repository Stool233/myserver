package org.stool.myserver.core.impl;

import org.stool.myserver.core.AsyncResult;
import org.stool.myserver.core.Future;
import org.stool.myserver.core.Handler;

import java.util.function.Function;

public class FutureImpl<T> implements Future<T> {

    @Override
    public void complete() {

    }

    @Override
    public void complete(T result) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public void fail(Throwable cause) {

    }

    @Override
    public void fail(String failureMessage) {

    }

    @Override
    public boolean tryComplete() {
        return false;
    }

    @Override
    public boolean tryComplete(T result) {
        return false;
    }

    @Override
    public boolean tryFail(Throwable cause) {
        return false;
    }

    @Override
    public boolean tryFail(String failureMessage) {
        return false;
    }

    @Override
    public Future<T> setHandler(Handler<AsyncResult<T>> handler) {
        return null;
    }

    @Override
    public <U> Future<U> map(Function<T, U> mapper) {
        return null;
    }

    @Override
    public Future<T> otherwise(Function<Throwable, T> mapper) {
        return null;
    }

    @Override
    public <U> Future<U> compose(Function<T, Future<U>> mapper) {
        return null;
    }

    @Override
    public T result() {
        return null;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public void handle(AsyncResult<T> event) {

    }
}
