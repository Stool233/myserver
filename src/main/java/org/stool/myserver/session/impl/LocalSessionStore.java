package org.stool.myserver.session.impl;

import org.stool.myserver.session.Session;
import org.stool.myserver.session.SessionStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalSessionStore implements SessionStore {

    private ConcurrentMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    public static LocalSessionStore create() {
        return new LocalSessionStore();
    }

    @Override
    public void put(String sessionId, Session session) {
        sessionMap.put(sessionId, session);
    }

    @Override
    public Session get(String sessionId) {
        return sessionMap.get(sessionId);
    }
}
