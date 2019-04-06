package com.vjsai.mini.mina.exceptions;

public class NioBaseWriteException extends Exception {
    public NioBaseWriteException() {
    }

    public NioBaseWriteException(String s) {
        super(s);
    }

    public NioBaseWriteException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NioBaseWriteException(Throwable throwable) {
        super(throwable);
    }

}
