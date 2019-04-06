package com.vjsai.mini.mina.commons.api;

public interface IoHandler {
    /**
     * Callback interface on what to do when a socket is opened
     * @param session
     */
    void onSocketOpen(final IoSession session);

    /**
     * callback on what to do when a socket is closed
     * @param session
     */
    void onSocketClose(final IoSession session);

    /**
     * Callback when a message is recieved
     * @param session
     * @param message
     */
    void onMessage(final IoSession session, final byte[] message);
}
