package org.stool.myserver.core;

import java.util.function.Function;

public interface Future<T> extends AsyncResult<T>, Handler<AsyncResult<T>> {

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
    <U> Future<U> map(Function<T, U> mapper);

    Future<T> otherwise(Function<Throwable, T> mapper);

    /**
     * 使用函数mapper对当前Future的result进行处理，返回一个新的Future
     * @param mapper
     * @param <U>
     * @return
     */
    <U> Future<U> compose(Function<T, Future<U>> mapper);


    static <T> Future<T> future() {
        return null;
    }

}
