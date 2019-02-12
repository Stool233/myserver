package org.stool.myserver.session;

public interface Session {

    void put(String key, Object value);

    <T> T get(String key);

    String id();
}
