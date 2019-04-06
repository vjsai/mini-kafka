package com.vjsai.mini.mina.commons.api;

public interface IoProcessor<S extends IoSession> {
    /**
     * Adds session to the queue
     * @param session
     */
    void add(S session);

    /**
     * destory the session
     * @param session
     */
    void destroy(S session);

    /**
     * Will tell whether the session is valid
     * @param session
     * @return
     */
    boolean isValid(S session);

    /**
     * Closes the session
     */
    void close();

}
