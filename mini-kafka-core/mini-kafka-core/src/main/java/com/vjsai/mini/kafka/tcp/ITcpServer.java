package com.vjsai.mini.kafka.tcp;

import com.vjsai.mini.kafka.messages.Message;

public interface ITcpServer {
    void onMessage(Message msg);
    void onOpen();
    void onClose();
}
