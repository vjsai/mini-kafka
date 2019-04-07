package com.vjsai.mini.mina.session;

import com.vjsai.mini.mina.api.IoFilter;
import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.api.IoProcessor;
import com.vjsai.mini.mina.api.IoSession;
import com.vjsai.mini.mina.constants.SessionStateConstants;
import com.vjsai.mini.mina.exceptions.NioBaseWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is responsible for handling and maintaining state of the socket sessions
 */
public class SocketSessionState implements IoSession, SessionStateConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketSessionState.class);

    private SocketChannel socketChannel;

    private SelectionKey selectionKey;

    private long sessionId;

    private IoFilter filter;

    private IoHandler handler;

    private int bufferSize;

    private int bufferPosition;

    private IoProcessor runnableProcessor;

    /**
     * Buffer for reading from channel
     */
    protected byte[] readBuffer;

    /**
     * A Queue for writing to the channel
     */
    protected Queue<byte[]> writeAbleBufferQueue = new ConcurrentLinkedQueue<byte[]>();

    /**
     * unique id for session to be used
     */
    private static AtomicLong uid = new AtomicLong(0);

    /**
     * The last active timestamp
     */
    private long lastActiveTimeStamp = System.currentTimeMillis();

    /**
     * The last timeout timestamp
     */
    private long lastTimeoutTimeStamp = System.currentTimeMillis();

    private long timeoutCount = 0;

    private long idleTimeoutInMs = DEFAULT_TIMEOUT_SPAN_MS;


    public SocketSessionState(SocketChannel socketChannel, IoFilter filter, IoHandler handler, int bufferSize) {
        this.socketChannel = socketChannel;
        this.filter = filter;
        this.handler = handler;
        this.bufferSize = calculateBufferSize(bufferSize);
        this.readBuffer = new byte[this.bufferSize];
    }

    public SocketSessionState(SocketChannel socketChannel, IoFilter filter, IoHandler handler, int bufferSize, long idleTimeoutInMs) {
        this.socketChannel = socketChannel;
        this.filter = filter;
        this.handler = handler;
        this.bufferSize = calculateBufferSize(bufferSize);
        this.readBuffer = new byte[this.bufferSize];
        this.idleTimeoutInMs = idleTimeoutInMs;
    }

    /**
     * to calculate buffer that needs to be allocated
     *
     * @param bufferSize
     * @return
     */
    private int calculateBufferSize(int bufferSize) {
        return (bufferSize <= MIN_BUFFER_SIZE || bufferSize >= MAX_BUFFER_SIZE) ? DEFAULT_BUFFER_SIZE : bufferSize + 4;
    }

    public long getSessionId() {
        return sessionId;
    }

    public IoHandler getHandler() {
        return handler;
    }

    public synchronized byte[] read() throws IOException {
        byte[] receivedBytes = null;
        receivedBytes = this.filter.filterReceive(this, receivedBytes);
        return receivedBytes;
    }

    public synchronized void write(final byte[] message) throws NioBaseWriteException, IOException {

        if (isClosing()) {
            throw new NioBaseWriteException("invalid session or session timeout. being destroyed ");
        }
        if (message == null || message.length == 0) {
            throw new NioBaseWriteException("not available data to write");
        }
        if (message.length > bufferSize - BOUND_BYTES_NUMBER) {
            throw new NioBaseWriteException(
                    "writeBytes length exceed session bufsize (buffersize - BOUND_BYTES_NUMBER)");
        }

        if (!runnableProcessor.isValid(this)) {
            throw new NioBaseWriteException("invalid session or session timeout. being destroyed ");
        }
        if (writeAbleBufferQueue.add(message)) {
            this.setWriteAble(!writeAbleBufferQueue.isEmpty());
        }
    }

    /**
     * Method to write to bufferqueue
     */
    public void writeLocal() {
        boolean isEmpty = writeAbleBufferQueue.isEmpty();
        //check if writable buffer queue is empty if so make it readable
        if (isEmpty) {
            this.setWriteAble(!isEmpty);
            return;
        }
        //If the queue is not empty apply writeFilter and remove from queue
        byte[] message = writeAbleBufferQueue.peek();
        if (message != null) {
            try {
                filter.writeFilter(this, message);
            } catch (NioBaseWriteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writeAbleBufferQueue.remove();
        }
    }

    public long getLastActiveTimeStamp() {
        return lastActiveTimeStamp;
    }

    public boolean isClosing() {
        //check if sessionKey is not valid if so rteurn true
        if (!selectionKey.isValid()) {
            LOGGER.debug("SessionKey is invalid session will be closed : " + this.toString());
            return true;
        }
        long currentTimeInMs = System.currentTimeMillis();
        //check if default timeout and idle timeout are elapsed
        if (DEFAULT_TIMEOUT_SPAN_MS >= (currentTimeInMs - this.lastActiveTimeStamp) && (this.timeoutCount >= DEFAULT_TIMEOUT_COUNT)) {
            LOGGER.debug("timeout condition met session will be closed : " + this.toString());
            return true;
        } else if ((currentTimeInMs - this.lastActiveTimeStamp) >= DEFAULT_MAX_IDLE_SPAN_MS) {
            LOGGER.debug("idle timeout met session will be closed : " + this.toString());
            return true;
        }
        return false;
    }

    /**
     * makes readbuffer empty,removes the filter and destorys the session
     */
    public synchronized void close() {
        readBuffer = null;
        this.filter.removeFilter();
        runnableProcessor.destroy(this);
    }

    public String getIPAddress() {
        return socketChannel.socket().getInetAddress().getHostAddress();
    }

    /**
     * Method to set Timeout flag
     */
    public synchronized void setTimeoutFlag() {

        long currentTimeMillis = System.currentTimeMillis();
        if (DEFAULT_TIMEOUT_SPAN_MS < (currentTimeMillis - this.lastTimeoutTimeStamp)
                && this.timeoutCount < this.DEFAULT_TIMEOUT_COUNT) {
            lastTimeoutTimeStamp = currentTimeMillis;
            timeoutCount = 0;
        } else if (DEFAULT_TIMEOUT_SPAN_MS > (currentTimeMillis - this.lastTimeoutTimeStamp)) {
            timeoutCount = timeoutCount >= DEFAULT_TIMEOUT_COUNT ? DEFAULT_TIMEOUT_COUNT : ++timeoutCount;
        }
    }

    /**
     * Sets to writable
     *
     * @param flag
     * @throws IllegalArgumentException
     * @throws CancelledKeyException
     */
    protected synchronized void setWriteAble(boolean flag) throws IllegalArgumentException, CancelledKeyException {
        if (selectionKey == null || !selectionKey.isValid()) {
            return;
        }
        int oldset = selectionKey.interestOps();
        int newset = oldset;
        if (flag) {
            newset |= SelectionKey.OP_WRITE;
        } else {
            newset &= ~SelectionKey.OP_WRITE;
        }
        if (newset != oldset) {
            selectionKey.interestOps(newset);
        }
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

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
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

    public void setRunnableProcessor(IoProcessor runnableProcessor) {
        this.runnableProcessor = runnableProcessor;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public byte[] getReadBuffer() {
        return readBuffer;
    }

    public int getBufferPosition() {
        return bufferPosition;
    }

    public void setBufferPosition(int bufferPosition) {
        this.bufferPosition = bufferPosition;
    }

    public void resetBuffer(){
        bufferPosition = 0;
        this.readBuffer[0] = (byte) 0;
        this.readBuffer[1] = (byte) 0;
    }

    public void setLastActiveTimeStamp(long lastActiveTimeStamp) {
        this.lastActiveTimeStamp = lastActiveTimeStamp;
    }
}
