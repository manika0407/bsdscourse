package com.project.bsds.bsdsassignment.message_queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bsds.bsdsassignment.db.Gender;
import com.project.bsds.bsdsassignment.db.State;
import com.project.bsds.bsdsassignment.db.UserInfo;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class ProducerTest {
  private static final String TOPIC_NAME = "stepcounter-test";
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

  public static void main(String[] args) {
    Properties confProps = new Properties();
    confProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093");
    confProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    confProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserStepEventSerializer.class.getName());
//    confProps.put("acks", "all");
//    confProps.put("retries", 0);
//    confProps.put("batch.size", 16384);
//    confProps.put("linger.ms", 1);
//    confProps.put("buffer.memory", 33554432);

    org.apache.kafka.clients.producer.Producer<String, UserStepEvent> producer = new KafkaProducer<>(confProps);
//    for (int i = 0; i < 10000; i++) {
    int num = 0;
    while (true) {
      ProducerRecord<String, UserStepEvent> record = new ProducerRecord<>(TOPIC_NAME, getRandomEvent());
      producer.send(record);
      num++;
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (num % 10 == 0) {
        System.out.println("Produced " + num + " records.");
      }
    }
//    producer.close();

//    confProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093");
//    confProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
//    confProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserStepEventSerializer.class.getName());
//    confProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "stepcounter-consumer");
//    confProps.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "stepCounter-consumer " + UUID.randomUUID().toString());
//    confProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(true));
//    confProps.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, Integer.toString(1000));
////    confProps.put("session.timeout.ms", "30000");
//
//    Consumer<String, UserStepEvent> consumer = new KafkaConsumer<>(confProps);
//    long startingOffset = -1; // always continue at end
//    consumer.subscribe(Arrays.asList(TOPIC_NAME), new ConsumerRebalanceListener() {
//      public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
//        System.out.printf("%s topic-partitions are revoked from this consumer\n", Arrays.toString(partitions.toArray()));
//      }
//      public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
//        System.out.printf("%s topic-partitions are assigned to this consumer\n", Arrays.toString(partitions.toArray()));
//        Iterator<TopicPartition> topicPartitionIterator = partitions.iterator();
//        while(topicPartitionIterator.hasNext()){
//          TopicPartition topicPartition = topicPartitionIterator.next();
//          System.out.println("Current offset is " + consumer.position(topicPartition) + " committed offset is ->" + consumer.committed(topicPartition) );
//          if(startingOffset == 0){
//            System.out.println("Setting offset to beginning");
//            consumer.seekToBeginning(Arrays.asList(topicPartition));
//          }else if(startingOffset == -1){
//            System.out.println("Setting it to the end ");
//            consumer.seekToEnd(Arrays.asList(topicPartition));
//          }else {
//            System.out.println("Resetting offset to " + startingOffset);
//            consumer.seek(topicPartition, startingOffset);
//          }
//        }
//      }
//    });
//    try {
//    while (true) {
//      ConsumerRecords<String, UserStepEvent> userStepEventConsumerRecords = consumer.poll(Duration.ofMillis(100));
//      for (ConsumerRecord<String, UserStepEvent> record : userStepEventConsumerRecords) {
//        System.out.printf("offset = %d, key = %s, value = %s\n", record.offset(), record.key(), record.value().toString());
//      }
//    }
//    } catch (WakeupException ex) {
//      System.out.println("Exiting consumer");
//    } finally{
//      consumer.close();
//    }
  }
}
