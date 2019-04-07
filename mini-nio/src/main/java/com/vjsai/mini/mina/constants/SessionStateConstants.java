package com.vjsai.mini.mina.constants;

public interface SessionStateConstants {

    int DEFAULT_BUFFER_SIZE = Short.MAX_VALUE + 4;

    int MIN_BUFFER_SIZE = 256 + 4;

    int MAX_BUFFER_SIZE = Integer.MAX_VALUE + 4;

    int DEFAULT_TIMEOUT_COUNT = 5;

    int DEFAULT_TIMEOUT_SPAN_MS = 30 * 1000;

    long DEFAULT_MAX_IDLE_SPAN_MS = Long.MAX_VALUE;

    int BOUND_BYTES_NUMBER = 4;
}
