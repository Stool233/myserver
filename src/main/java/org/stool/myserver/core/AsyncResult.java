package org.stool.myserver.core;

import java.util.function.Function;

/**
 * 异步结果
 * @param <T> 异步结果包含的类型
 */
public interface AsyncResult<T> {

    T result();

    boolean succeeded();

    boolean failed();

    /**
     * @return 当执行失败的时候，具体的错误信息
     */
    Throwable cause();

    /**
     * 对当前的异步结果执行一个mapper函数，返回一个新的异步结果
     * @param mapper 执行的函数
     * @param <U> 新的异步结果的类型
     * @return 新的异步结果
     */
    <U> AsyncResult<U> map(Function<T, U> mapper);

    /**
     * 当执行失败时，对当前的异步结果执行一个mapper函数，返回一个新的异步结果，表示错误处理的逻辑
     * @param mapper
     * @return
     */
    AsyncResult<T> otherwise(Function<Throwable, T> mapper);


}
