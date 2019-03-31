package com.vjsai.mini.kafka.messages;

public enum MessageType {

    // Producer
    CREATE_TOPIC("createTopic"),
    PUBLISH_MESSAGE("publishMessage"),

    // Broker
    GET_TOPIC("assignTopic"),
    GET_TOPIC_FOR_COORDINATOR("getTopicPartitions"),
    TOPIC_ASSIGNMENT_TO_PRODUCER("updateTopicPartitionLeader"),
    PUBLISH_MESSAGE_ACK("publishMessageAck"),
    ACK(""),

    //ZooKeeper
    TOPIC_ASSIGNMENT_TO_BROKER("assignTopicToProducer"),
    SET_TOPIC_PARTITION_LEADER("setTopicPartitionLeader"),

    //Consumer
    FIND_COORDINATOR("getCoordinator"),
    UPDATE_COORDINATOR("updateCoordinator"),
    JOIN_GROUP("addConsumerToGroup"),
    SUBSCRIBE_TOPIC("storeInfoAndGetTopicAndRebalance"),
    PULL_MESSAGE("giveMessage"),
    SEND_MESSAGE_TO_CONSUMER("dealWithMessage");
    private String messageMame;

    private MessageType(String name) {
        this.messageMame = name;
    }

    @Override
    public String toString(){
        return messageMame;
    }
}

