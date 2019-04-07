package com.vjsai.mini.mina.process;

import com.vjsai.mini.mina.api.IoProcessor;
import com.vjsai.mini.mina.exceptions.ProcessorPoolException;
import com.vjsai.mini.mina.session.SocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * this class takes care of implmenting process pool
 */
public class NioProcessPool implements IoProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NioProcessPool.class);
    /**
     * Processor pool
     */
    private IoProcessor processorPool[];
    /**
     * pool size
     */
    private int poolSize = 5;

    /**
     * Executor thread
     */
    private final Executor executor;

    public NioProcessPool(Class<? extends IoProcessor> classType, int poolSize) throws ProcessorPoolException{
        this.poolSize = poolSize;
        this.processorPool = new IoProcessor[poolSize];
        executor = Executors.newCachedThreadPool();
        //get constructor of class
        Constructor<? extends IoProcessor> constructor = null;

        for (int i = 0; i < poolSize; i++) {
            try {
                //Initialize all the instances
                constructor = classType.getConstructor(Executor.class);
                processorPool[i] = constructor.newInstance(this.executor);
            } catch (Exception e) {
                LOGGER.warn("An exception occured while initializing thread pool: ", e);
                throw new ProcessorPoolException("An exception occured while initializing thread pool");
            }
        }

    }

    public void add(SocketSessionState session) {
        IoProcessor processor = getProcessor(session);
        session.setRunnableProcessor(processor);
        processor.add(session);
    }

    //pick one from the processorPool
    private IoProcessor getProcessor(SocketSessionState session) {
        return processorPool[(int) (session.getSessionId() % poolSize)];
    }

    public void close() {
        for (IoProcessor processor : processorPool) {
            processor.close();
        }
        ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (Exception e) {
            LOGGER.warn("unable to shutdown properly");
        }
    }

    //Need to implement these
    public void destroy(SocketSessionState session) {

    }

    public boolean isValid(SocketSessionState session) {
        return false;
    }
}
