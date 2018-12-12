package com.project.bsds.bsdsassignment.message_queue;

import com.project.bsds.bsdsassignment.db.Gender;
import com.project.bsds.bsdsassignment.db.State;
import com.project.bsds.bsdsassignment.db.UserInfo;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;

public class UserStepEventProducer implements Closeable {
  private static final String TOPIC_NAME = "stepcounter_test_topic";
  private static final String INIT_BROKERS_LIST =
  "34.218.47.83:9092,34.220.41.48:9092,54.203.1.137:9092";
//      "localhost:9092,localhost:9093";
  private static final boolean SEND_ASYNC = true;

  private Producer<Integer, UserStepEvent> producer;

  public UserStepEventProducer() {
    Properties confProps = new Properties();
    confProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, INIT_BROKERS_LIST);
    confProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
    confProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserStepEventSerializer.class.getName());
    confProps.put(ProducerConfig.ACKS_CONFIG, "1"); // only wait for leader ack
    confProps.put(ProducerConfig.RETRIES_CONFIG, 0); // for no duplicates
    confProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024); // send buffer size
    confProps.put(ProducerConfig.LINGER_MS_CONFIG, 1); // wait between sends
    confProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 128 * 1024 * 1024); // block if buffer full

    producer = new KafkaProducer<>(confProps);
  }

  public void addUserStepEvent(final UserStepEvent userStepEvent) throws IOException {
    ProducerRecord<Integer, UserStepEvent> record = new ProducerRecord<>(TOPIC_NAME, userStepEvent.getUserId(), userStepEvent);
    Future<RecordMetadata> fut =
        producer.send(
            record,
            (recordMetadata, e) -> {
              if (e != null) {
                System.err.println("Failed to upload event: " + userStepEvent.toString());
              } else {
//                System.out.println(
//                    "UserStepEvent sent to topic ->"
//                        + recordMetadata.topic()
//                        + " ,parition->"
//                        + recordMetadata.partition()
//                        + " stored at offset->"
//                        + recordMetadata.offset());
              }
            });
    if (!SEND_ASYNC) {
      try {
        fut.get();
      } catch (Exception e) {
        throw new IOException("can't send event", e);
      }
    }
  }

  @Override
  public void close() throws IOException {
    producer.flush();
    producer.close();
  }

  private static final Random RANDOM = new Random();
  static UserStepEvent getRandomEvent() {
    return new UserStepEvent(
        new UserInfo(
            RANDOM.nextInt(5000),
            State.values()[RANDOM.nextInt(State.LAST.ordinal())],
            Gender.values()[RANDOM.nextInt(2)]),
        RANDOM.nextInt(31),
        RANDOM.nextInt(24),
        RANDOM.nextInt(1000));
  }
  public static void main(String[] args) throws IOException {
    UserStepEventProducer userStepEventProducer = new UserStepEventProducer();
    for (int i = 0; i < 100000; i++) {
      userStepEventProducer.addUserStepEvent(getRandomEvent());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (i % 10 == 0) {
        System.out.println("sent " + i + " events");
      }
    }
  }
}
