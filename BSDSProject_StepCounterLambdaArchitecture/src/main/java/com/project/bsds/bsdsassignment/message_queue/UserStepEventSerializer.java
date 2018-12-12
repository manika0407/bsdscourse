package com.project.bsds.bsdsassignment.message_queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class UserStepEventSerializer implements Closeable, AutoCloseable, Serializer<UserStepEvent>, Deserializer<UserStepEvent> {
  private ObjectMapper objectMapper;

  public UserStepEventSerializer() {
    this(null);
  }

  public UserStepEventSerializer(ObjectMapper mapper) {
    this.objectMapper = mapper;
  }

  @Override
  public void configure(Map<String, ?> map, boolean b) {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();
    }
  }

  @Override
  public byte[] serialize(String s, UserStepEvent userStepEvent) {
    try {
      return objectMapper.writeValueAsBytes(userStepEvent);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public UserStepEvent deserialize(String s, byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, UserStepEvent.class);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void close() {
    objectMapper = null;
  }
}