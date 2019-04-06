package com.vjsai.mini.mina.commons.api;

import com.vjsai.mini.mina.exceptions.NioBaseWriteException;

import java.io.IOException;

public interface IoSession {
    /**
     * get unique identifier for session id
     * @return
     */
    long getSessionid();

    /**
     * get handler for state
     * @return
     */
    IoHandler getHandler();

    /**
     * write bytes to state
     * @param message
     * @throws NioBaseWriteException
     * @throws IOException
     */
    void write(final byte[] message) throws NioBaseWriteException, IOException;

    /**
     * Get last time when the session was active
     * @return
     */
    long getLastActiveTimeStamp();

    /**
     * if the session is clsoing
     */
    boolean isClosing();

    /**
     * for clsoing the session
     */
    void close();

    String toString();

}
