package com.vjsai.mini.mina.utils;

public class NamedRunnable implements Runnable {
    //unique identifier for thread
    private final String threadName;
    private final Runnable runnable;

    public NamedRunnable(String threadName, Runnable runnable) {
        this.threadName = threadName;
        this.runnable = runnable;
    }

    public void run() {
        Thread currentT = Thread.currentThread();
        String rawName = currentT.getName();
        if (threadName != null) {
            currentT.setName(this.threadName);
        } else {
            currentT.setName(rawName);
        }
        runnable.run();
    }
}
