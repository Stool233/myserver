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
    default <U> AsyncResult<U> map(Function<T, U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        return new AsyncResult<U>() {
            @Override
            public U result() {
                if (succeeded()) {
                    return mapper.apply(AsyncResult.this.result());
                } else {
                    return null;
                }
            }

            @Override
            public boolean succeeded() {
                return AsyncResult.this.succeeded();
            }

            @Override
            public boolean failed() {
                return AsyncResult.this.failed();
            }

            @Override
            public Throwable cause() {
                return AsyncResult.this.cause();
            }

        };
    }

    /**
     * 当执行失败时，对当前的异步结果执行一个mapper函数，返回一个新的异步结果，表示错误处理的逻辑
     * @param mapper
     * @return
     */
    default AsyncResult<T> otherwise(Function<Throwable, T> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        return new AsyncResult<T>() {
            @Override
            public T result() {
                if (AsyncResult.this.succeeded()) {
                    return AsyncResult.this.result();
                } else if (AsyncResult.this.failed()) {
                    return mapper.apply(AsyncResult.this.cause());
                } else {
                    return null;
                }
            }

            @Override
            public boolean succeeded() {
                return AsyncResult.this.succeeded() || AsyncResult.this.failed();
            }

            @Override
            public boolean failed() {
                return false;
            }

            @Override
            public Throwable cause() {
                return null;
            }
        };
    }


}
