package com.vjsai.mini.mina.api;

import com.vjsai.mini.mina.session.SocketSessionState;


public interface IoProcessor {
    /**
     * Adds session to the queue
     * @param session
     */
    void add(SocketSessionState session);

    /**
     * destory the session
     * @param session
     */
    void destroy(SocketSessionState session);

    /**
     * Will tell whether the session is valid
     * @param session
     * @return
     */
    boolean isValid(SocketSessionState session);

    /**
     * Closes the session
     */
    void close();

}
