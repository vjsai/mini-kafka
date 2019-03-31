package com.vjsai.mini.kafka.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private final MessageType methodName;
    private final List<Object> args;
    private final boolean isAck;

    public Message(MessageType methodName, List<Object> args, boolean isAck) {
        this.methodName = methodName;
        this.args = args;
        this.isAck = isAck;
    }

    public Message(MessageType methodName) {
        this.methodName = methodName;
        this.args = new ArrayList<Object>();
        this.isAck = false;
    }

    public MessageType getMethodName() {
        return methodName;
    }

    public List<Object> getArgs() {
        return args;
    }

    public boolean isAck() {
        return isAck;
    }
}
