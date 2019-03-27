package com.vjsai.mini.kafka.topic;

import java.io.Serializable;

public class Topic implements Serializable {
  private final String topicName;
  private final int key;
  private final int partition;
  private final int replication;

  public Topic(String topicName, int key, int partition, int replication) {
    this.topicName = topicName;
    this.key = key;
    this.partition = partition;
    this.replication = replication;
  }

  public String getName() {
    return topicName;
  }
}
