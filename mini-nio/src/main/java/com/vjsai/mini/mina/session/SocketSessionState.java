package com.vjsai.mini.mina.session;

import com.vjsai.mini.mina.api.IoFilter;
import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.api.IoSession;
import com.vjsai.mini.mina.constants.SessionStateConstants;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is responsible for handling and maintaining state of the socket sessions
 */
public class SocketSessionState implements IoSession, SessionStateConstants {

    private SocketChannel socketChannel;

    private SelectionKey selectionKey;

    private long sessionid;

    private IoFilter filter;

    private IoHandler handler;

    private int bufferSize;

    /**
     *  Buffer for reading from channel
     */
    protected byte[] readBuffer;

    /**
     *  A Queue for writing to the channel
     */
    protected Queue<byte[]> writeAbleQueue = new ConcurrentLinkedQueue<byte[]>();

    /**
     * unique id for session to be used
     */
    private static AtomicLong uid = new AtomicLong(0);

    public SocketSessionState(SocketChannel socketChannel, IoFilter filter, IoHandler handler, int bufferSize) {
        this.socketChannel = socketChannel;
        this.filter = filter;
        this.handler = handler;
        this.bufferSize = bufferSize;
    }

    public long getSessionid() {
        return 0;
    }

    public IoHandler getHandler() {
        return null;
    }

    public void write(byte[] message) throws NioBaseWriteException, IOException {

    }

    public long getLastActiveTimeStamp() {
        return 0;
    }

    public boolean isClosing() {
        return false;
    }

    public void close() {

    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public void setSessionid(long sessionid) {
        this.sessionid = sessionid;
    }

    public IoFilter getFilter() {
        return filter;
    }

    public void setFilter(IoFilter filter) {
        this.filter = filter;
    }

    public void setHandler(IoHandler handler) {
        this.handler = handler;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
