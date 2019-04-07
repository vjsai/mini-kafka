package com.vjsai.mini.mina.polling;

import com.vjsai.mini.mina.api.IoFilterChain;
import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.api.IoProcessor;
import com.vjsai.mini.mina.exceptions.HandlerNotDefinedException;
import com.vjsai.mini.mina.exceptions.ProcessorPoolException;
import com.vjsai.mini.mina.filters.BaseFilterChain;
import com.vjsai.mini.mina.filters.DefaultTcpFilter;
import com.vjsai.mini.mina.process.NioProcessPool;
import com.vjsai.mini.mina.session.SocketSessionState;
import com.vjsai.mini.mina.utils.NamedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This abstracts PollingIo for server
 */
public abstract class AbstractPollingIoAcceptor {

    private final Logger logger = LoggerFactory.getLogger(AbstractPollingIoAcceptor.class);
    /**
     * thread pool executor
     */
    private final Executor executor;

    /**
     * callback handler
     */
    private IoHandler handler = null;

    /**
     * TCP client I/O processing sub thread (fixed size pool)
     */
    private IoProcessor processor;

    /**
     * Chain of responsibility for byte stream processing
     */
    protected IoFilterChain filterChain;

    /**
     * socket client 's idle time by millisecond
     */
    private long maxSessionIdleTimeoutInMS = 0;

    /**
     * socket client read-buffer size
     */
    private int sessionBufferSize = 0;

    /**
     * inner thread object reference
     */
    private AtomicReference<Acceptor> atomicAcceptor = new AtomicReference<Acceptor>();

    /**
     * thread counter
     */
    private static AtomicLong uid = new AtomicLong(0);

    /**
     * current thread name
     */
    private String threadName;

    //initialize stoprun to flase
    protected volatile boolean stopRun = false;

    //the max processor size
    private static int JVM_PROCESSOR_COUNT = 0;

    private final static int SELECTOR_MS = 1;

    /**
     * inner method for listening
     *
     * @param sa
     * @return
     * @throws IOException
     */
    protected abstract void bindLocal(SocketAddress sa) throws IOException;

    /**
     * polling server main channel
     *
     * @return
     * @throws IOException
     */
    protected abstract int select(int selectms) throws ClosedSelectorException, IOException;

    /**
     * return ready-set
     *
     * @return
     */
    protected abstract Iterator<SelectionKey> selectKeys();

    /**
     * initialize server socket
     *
     * @throws IOException
     */
    protected abstract void init() throws IOException;

    /**
     * accept socket client method
     *
     * @param selectionKey
     * @return
     * @throws IOException
     */
    protected abstract SocketSessionState accept(SelectionKey selectionKey) throws IOException;

    public AbstractPollingIoAcceptor(Class<? extends IoProcessor> processorType, Executor executor,
                                     int processorSize) throws ProcessorPoolException {
        if (executor == null) {
            this.executor = Executors.newCachedThreadPool();
        } else {
            this.executor = executor;
        }
        this.filterChain = new BaseFilterChain();
        this.filterChain.addFilter("DEFAULT", new DefaultTcpFilter());
        this.processor = new NioProcessPool(processorType, processorSize);
        this.threadName = getClass().getSimpleName() + "-" + uid.incrementAndGet();
    }

    public AbstractPollingIoAcceptor(Class<? extends IoProcessor> processorType, Executor executor)
            throws ProcessorPoolException {
        this(processorType, executor, JVM_PROCESSOR_COUNT);
    }

    /**
     * get current JVM available processor count
     */
    static {
        int count = Runtime.getRuntime().availableProcessors();
        JVM_PROCESSOR_COUNT = (count <= 0 || count >= 4) ? 2 : count + 1;
    }

    /***
     * bind a host:port to loopback address & start
     *
     * @param socketAddress
     * @throws HandlerNotDefinedException
     * @throws IOException
     */
    public final void bind(SocketAddress socketAddress) throws HandlerNotDefinedException, IOException {

        if (handler == null) {
            throw new HandlerNotDefinedException("Handler No Defined");
        }

        try {
            init();
            bindLocal(socketAddress);
        } catch (IOException e) {
            logger.warn("bind : ", e);
            throw e;
        }
        //start acceptor thread
        startAcceptor();
    }

    /**
     * start up acceptor thread
     */
    private void startAcceptor() {
        Acceptor acceptor = atomicAcceptor.get();
        if (acceptor == null) {
            acceptor = new Acceptor();
            atomicAcceptor.set(acceptor);
            executor.execute(new NamedRunnable(this.threadName,acceptor));
        }
    }

    /**
     * shutdown the server
     */
    protected void shutdown() {
        this.stopRun = true;
        try {
            this.processor.close();
        } catch (IOException e) {
            logger.error("Shutdown server : " + e.toString(), e);
        }
        ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (Exception e) {
        }
    }

    /**
     * set main callback handler
     * @param handler
     * @throws HandlerNotDefinedException
     */
    public void setHandler(final IoHandler handler) throws HandlerNotDefinedException {
        if (handler == null) {
            throw new HandlerNotDefinedException("Handler No Defined");
        }
        this.handler = handler;
    }

    public IoHandler getHandler() {
        return this.handler;
    }

    /**
     * process the OP_ACCEPT event, and then initialize a TCP session, After that,
     * dispatch the session to one processor thread
     *
     * @param iterator
     * @throws IOException
     */
    private void processConnection(Iterator<SelectionKey> iterator) throws IOException {
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            SocketSessionState session = accept(selectionKey);
            if (session != null) {
                processor.add(session);
            }
        }
    }

    public long getMaxSessionIdleTimeoutInMS() {
        return maxSessionIdleTimeoutInMS;
    }

    /**
     * set socket state session 's max idle time span(millisecond)
     * @param maxSessionIdleTimeoutInMS
     */
    public void setMaxSessionIdleTimeoutInMS(long maxSessionIdleTimeoutInMS) {
        this.maxSessionIdleTimeoutInMS = maxSessionIdleTimeoutInMS;
    }

    public int getSessionBufferSize() {
        return sessionBufferSize;
    }

    /**
     * set socket state session's channel buffer size (in bytes)
     */
    public void setSessionBufferSize(int sessionBufferSize) {
        this.sessionBufferSize = sessionBufferSize;
    }

    public IoFilterChain getFilterChain() {
        return this.filterChain;
    }

    public void setFilterChain(IoFilterChain chain) {
        this.filterChain = chain;
    }

    /***
     * Acceptor polling inner thread class
     */
    private class Acceptor implements Runnable {
        public void run() {
            logger.debug("acceptor thread starting up ... ... ...");
            while (!stopRun) {
                try {
                    if (select(SELECTOR_MS) > 0) {
                        processConnection(selectKeys());
                    }
                } catch (ClosedSelectorException e) {
                    logger.error("acceptor :", e);
                    break;
                } catch (Exception e) {
                    logger.error("acceptor :", e);
                }
            }
            logger.debug("acceptor thread shutting down ... ... ...");
        }
    }
}