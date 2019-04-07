package com.vjsai.mini.mina.nio;

import com.vjsai.mini.mina.polling.AbstractPollingIoProcessor;
import com.vjsai.mini.mina.session.SocketSessionState;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;


public final class NioProcessor extends AbstractPollingIoProcessor {

    private Selector selector;

    public NioProcessor(Executor executor) throws IOException {
        super(executor);
        selector = Selector.open();
    }

    /**
     * intializes the session for reading
     * @param session
     * @return
     * @throws IOException
     */
    protected boolean init(SocketSessionState session) throws IOException {
        SocketChannel socketChannel = session.getSocketChannel();
        if (socketChannel != null) { // null check
            if (!socketChannel.isConnected()) {
                return false;
            }
            socketChannel.socket().setReuseAddress(true);
            socketChannel.configureBlocking(false);
            SelectionKey sk = socketChannel.register(selector, SelectionKey.OP_READ);
            session.setSelectionKey(sk);
            sk.attach(session);
        }
        return true;
    }

    @Override
    public int select(int selectMs) throws ClosedSelectorException,IOException {
        return selector.select(selectMs);
    }

    @Override
    public void wakeup() {
        selector.wakeup();
    }

    @Override
    protected Iterator<SelectionKey> selectKeys() {
        return selector.selectedKeys().iterator();
    }

    /**
     * read from session and return recieved bytes
     * @param session
     * @return
     * @throws IOException
     */
    protected byte[] read(SocketSessionState session) throws IOException {
        byte[] receivedBytes = null;
        receivedBytes = session.read();
        return receivedBytes;
    }

    protected void write(SocketSessionState session) {
        session.writeLocal();
    }

    /**
     * while closing stop the current selector
     * @throws IOException
     */
    public void close() throws IOException {
        this.stopRun = true;
        this.selector.wakeup();
        this.selector.close();
    }
}