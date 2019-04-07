package com.vjsai.mini.mina.polling;

import com.vjsai.mini.mina.api.IoProcessor;
import com.vjsai.mini.mina.session.SocketSessionState;
import com.vjsai.mini.mina.utils.NamedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractPollingIoProcessor implements IoProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractPollingIoProcessor.class);

    private final static int SELECTOR_MS = 1;

    protected Executor executor = null;

    private AtomicReference<Processor> processorReference = new AtomicReference<Processor>();

    /**
     * for maintaining session register queue
     */
    private Queue<SocketSessionState> sessionRegisterQueue = new ConcurrentLinkedQueue<SocketSessionState>();

    /**
     *  A queue for maintianing current valid session
     */
    private Queue<SocketSessionState> sessionCurrentValidQueue = new ConcurrentLinkedQueue<SocketSessionState>();

    /**
     * Queue for maintaining unregister queue
     */
    private Queue<SocketSessionState> sessionUnregisterQueue = new ConcurrentLinkedQueue<SocketSessionState>();

    private static AtomicLong uid = new AtomicLong(0);

    private String threadName = "";

    /**
     * whether existing TCP socket client disconnection
     */
    private AtomicBoolean isUnregister = new AtomicBoolean(false);

    protected volatile boolean stopRun = false;

    protected abstract Iterator<SelectionKey> selectKeys();

    /**
     * For initilazing a new session
     *
     * @param session
     * @return
     * @throws IOException
     */
    protected abstract boolean init(SocketSessionState session) throws IOException;

    protected abstract int select(int selectMs) throws ClosedSelectorException, IOException;

    protected abstract void wakeup();

    /**
     * To read from socket session state
     * @param session
     * @return
     * @throws IOException
     */
    protected abstract byte[] read(SocketSessionState session) throws IOException;

    /**
     * @param session
     */
    protected abstract void write(SocketSessionState session);

    /**
     *
     * @param executor
     */
    public AbstractPollingIoProcessor(Executor executor) {
        this.executor = executor;
        this.threadName = getClass().getSimpleName() + "-" + uid.incrementAndGet();
    }

    /**
     * When a session is added to session register queue start processing
     * @param session
     */
    public void add(SocketSessionState session) {
        if (session != null) {
            sessionRegisterQueue.add(session);
            startProcessor();
        }
    }

    /**
     * Start processor ie., start executing thread
     */
    private void startProcessor() {
        Processor processor = processorReference.get();
        if (processor == null) {
            processor = new Processor();
            processorReference.set(processor);
            executor.execute(new NamedRunnable(this.threadName, processor));
        }
    }

    /**
     * Intializes the new session basing upon the sessions in sessionRegisterQueue
     * @throws IOException
     */
    private void registerNewSession() throws IOException {
        int size = sessionRegisterQueue.size();
        for (int i = 0; i < size; i++) {
            SocketSessionState session = sessionRegisterQueue.poll();
            if (init(session)) {
                sessionCurrentValidQueue.add(session);
                session.getHandler().onSocketOpen(session);
            }
        }
    }

    /**
     * Add the session to unreister queue and then remove from current valid queue
     * @param session
     */
    public synchronized void destroy(SocketSessionState session) {
        sessionUnregisterQueue.add(session);
        sessionCurrentValidQueue.remove(session);
        isUnregister.set(true);
    }

    /**
     * Takes all the sessions in the unregister queue and closes them
     */
    private void unregisterSession() {

        int size = sessionUnregisterQueue.size();
        for (int i = 0; i < size; i++) {
            //poll is used as it returns null where as remove throws exception
            SocketSessionState session = sessionUnregisterQueue.poll();
            SelectionKey sessionSelectionKey = session.getSelectionKey();
            try {
                sessionSelectionKey.channel().close();
            } catch (IOException e) {
                LOGGER.error("Exception : while closing the session" + session, e);
            }
            sessionSelectionKey.cancel();
            LOGGER.debug("session close: " + session);
            session.getHandler().onSocketClose(session);
            session = null;
        }
        isUnregister.set(false);
    }

    /**
     * process iterates over select keys and processes basing on read or write mode
     */
    void process() {
        Iterator<SelectionKey> iterator = selectKeys();
        while (iterator.hasNext()) {

            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            SocketSessionState session = (SocketSessionState) selectionKey.attachment();
            //if selected is readable try reading from session
            if (selectionKey.isValid() && selectionKey.isReadable()) {

                // invalid session
                if (!this.isValid(session)) {
                    continue;
                }

                byte[] bArray = null;
                //try reading from session incase of error set the timeout
                try {
                    bArray = read(session);
                } catch (IOException e) {
                    session.setTimeoutFlag();
                }
                //If able to read successfully invoke onMessageHandler
                if (bArray != null) {
                    session.getHandler().onMessage(session, bArray);
                }

            } else //check if needs to written then write to session
                if (selectionKey.isValid() && selectionKey.isWritable()) {
                write(session);
            }
        }
    }

    /**
     * check for status of sessions
     */
    private void checkSessionsStatus() {
        //iterate through all the sessions and destory which ever sessions are closing
        for (SocketSessionState session : sessionCurrentValidQueue) {
            if (session.isClosing()) {
                this.destroy(session);
            }
        }

    }

    public synchronized boolean isValid(SocketSessionState session) {
        //check if session exists in valid queue
        if (!sessionCurrentValidQueue.contains(session)) {
            //check if session exists in unregister queue
            if (!sessionUnregisterQueue.contains(session)) {
                sessionUnregisterQueue.add(session);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Inner Processor class
     */
    private class Processor implements Runnable {

        public void run() {

            LOGGER.debug("processor thread starting up ... ... ...");
            while (!stopRun) {
                try {
                    //check current status of session
                    checkSessionsStatus();
                    //current  selected
                    int selected = select(SELECTOR_MS);
                    //register new sessions
                    registerNewSession();
                    //if the selected is valid start process
                    if (selected > 0) {
                        process();
                    }
                    //if is unreqister flag is set start unregistering the sessions from unregister queue
                    if (isUnregister.get()) {
                        unregisterSession();
                    }

                } catch (ClosedSelectorException e) {
                    LOGGER.error("processor: " + e.toString(), e);
                    break;
                } catch (Exception e) {
                    LOGGER.error("processor: " + e.toString(), e);
                }
            }
            LOGGER.debug("processor thread shuting down ... ... ...");
        }
    }
}