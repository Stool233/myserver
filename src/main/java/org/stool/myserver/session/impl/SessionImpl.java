package org.stool.myserver.session.impl;

import org.stool.myserver.session.Session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionImpl implements Session {

    private ConcurrentMap<String, Object> data = new ConcurrentHashMap<>();

    private String id;

    public SessionImpl() {
        id = UUID.randomUUID().toString();
    }

    @Override
    public void put(String key, Object value) {
        data.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @Override
    public String id() {
        return id;
    }
}
