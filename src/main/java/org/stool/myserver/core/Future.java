package org.stool.myserver.core;

import io.netty.channel.Channel;
import org.stool.myserver.core.impl.FailedFuture;
import org.stool.myserver.core.impl.FutureImpl;
import org.stool.myserver.core.impl.SucceededFuture;


import java.util.function.Function;

public interface Future<T> extends AsyncResult<T>, Handler<AsyncResult<T>> {

    static final SucceededFuture EMPTY = new SucceededFuture<>(null);

    static <T> Future<T> future(Handler<Future<T>> handler) {
        Future<T> fut = future();
        handler.handle(fut);
        return fut;
    }

    static <T> Future<T> future() {
        return new FutureImpl<>();
    }

    static <T> Future<T> succeededFuture() {
        Future<T> fut = EMPTY;
        return fut;
    }

    static <T> Future<T> failedFuture(Throwable cause) {
        return new FailedFuture<T>(cause);
    }


    static  <T> Future<T> succeededFuture(T result) {
        return new SucceededFuture<>(result);
    }



    /**
     * 设置result，并且调用handler
     */
    void complete();

    void complete(T result);

    boolean isComplete();

    void fail(Throwable cause);

    void fail(String failureMessage);

    boolean tryComplete();

    boolean tryComplete(T result);

    boolean tryFail(Throwable cause);

    boolean tryFail(String failureMessage);

    Future<T> setHandler(Handler<AsyncResult<T>> handler);

    /**
     * 对当前的Future执行一个mapper函数，返回一个新的Future
     * @param mapper 执行的函数
     * @param <U> 新的异步结果的类型
     * @return 新的Future
     */
    default <U> Future<U> map(Function<T, U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<U> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                U mapped;
                try {
                    mapped = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(mapped);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }

    default Future<T> otherwise(Function<Throwable, T> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<T> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                ret.complete(result());
            } else {
                T value;
                try {
                    value = mapper.apply(ar.cause());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(value);
            }
        });
        return ret;
    }

    /**
     * 使用函数mapper对当前Future的result进行处理，返回一个新的Future
     * @param mapper
     * @param <U>
     * @return
     */
    default <U> Future<U> compose(Function<T, Future<U>> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<U> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                Future<U> apply;
                try {
                    apply = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return ;
                }
                apply.setHandler(ret);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }


    Handler<AsyncResult<T>> getHandler();
}
