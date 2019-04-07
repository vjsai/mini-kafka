package com.vjsai.mini.mina.api;

import com.vjsai.mini.mina.exceptions.NioBaseWriteException;
import com.vjsai.mini.mina.session.SocketSessionState;

import java.io.IOException;

public interface IoFilter {
    /**
     *
     * @param session
     * @param filterBytes
     * @return
     * @throws IOException
     */
    byte[] filterReceive(SocketSessionState session, byte[] filterBytes) throws  IOException;

    /**
     * write Filter
     * @param session
     * @param writeBytes
     * @return
     * @throws NioBaseWriteException
     * @throws IOException
     */
    void writeFilter(SocketSessionState session, byte[] writeBytes) throws NioBaseWriteException, IOException;

    /**
     * Removes the filter
     */
    void removeFilter();

}
