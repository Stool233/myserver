package org.stool.myserver.session;

public interface SessionStore {

    void put(String sessionId, Session session);

    Session get(String sessionId);
}
