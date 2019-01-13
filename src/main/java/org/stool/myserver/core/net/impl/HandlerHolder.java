package org.stool.myserver.core.net.impl;

import org.stool.myserver.core.Context;

import java.util.Objects;

/**
 * Context与对应handler的包装
 * @param <T>
 */
public class HandlerHolder<T> {

    public final Context context;
    public final T handler;

    public HandlerHolder(Context context, T handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandlerHolder<?> that = (HandlerHolder<?>) o;
        return Objects.equals(context, that.context) &&
                Objects.equals(handler, that.handler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, handler);
    }
}
