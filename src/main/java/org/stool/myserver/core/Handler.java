package org.stool.myserver.core;

/**
 * 处理事件的逻辑的抽象
 * @param <E>
 */
public interface Handler<E> {

    void handle(E event);
}
