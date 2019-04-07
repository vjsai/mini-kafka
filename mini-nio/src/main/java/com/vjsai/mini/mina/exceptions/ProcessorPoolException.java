package com.vjsai.mini.mina.exceptions;

public class ProcessorPoolException  extends Exception{

    public ProcessorPoolException() {
        super();
    }

    public ProcessorPoolException(String s) {
        super(s);
    }

    public ProcessorPoolException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ProcessorPoolException(Throwable throwable) {
        super(throwable);
    }
}
