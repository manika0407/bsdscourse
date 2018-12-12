// Copyright 2012-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// Licensed under the Apache License, Version 2.0.

package com.project.bsds.bsdsassignment.db;

import java.util.Arrays;
import java.util.HashMap;

import com.amazonaws.client.builder.AwsClientBuilder;  // remove import
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class DynamoDbTest {
  final static String TABLE_NAME = "stepcounter_test";

  static void createTable(final DynamoDB dynamoDB) throws Exception {
    try {
      System.out.println("Attempting to create table; please wait...");
      Table table = dynamoDB.createTable(TABLE_NAME,
          Arrays.asList(new KeySchemaElement("user_id", KeyType.HASH), // Partition
              // key
              new KeySchemaElement("day", KeyType.RANGE)), // Sort key
          Arrays.asList(new AttributeDefinition("user_id", ScalarAttributeType.N),
              new AttributeDefinition("day", ScalarAttributeType.N)),
          new ProvisionedThroughput(10L, 10L));
      table.waitForActive();
      System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());

    } catch (Exception e) {
      System.err.println("Unable to create table: ");
      System.err.println(e.getMessage());
      throw e;
    }
  }

  static void writeData(final DynamoDB dynamoDB) throws Exception {
    Table table = dynamoDB.getTable(TABLE_NAME);
    int userId = 1;
    int day = 1;
    int ti = 1;
    int sc = 500;
    try {
      table.putItem(
          new Item()
              .withPrimaryKey("user_id", userId, "day", day)
              .withInt("time_interval", ti)
              .withInt("step_count", sc));
      System.out.println("PutItem succeeded: " + userId + " " + day + " " + ti + " " + sc);
    } catch (Exception e) {
      System.err.println(
          "Unable to add stepcount entry: " + userId + " " + day + " " + ti + " " + sc);
      System.err.println(e.getMessage());
      throw e;
    }
  }

  static void getData(final DynamoDB dynamoDB) throws Exception {
    Table table = dynamoDB.getTable(TABLE_NAME);
    int userId = 1;
    int day = 2;
//    GetItemSpec spec = new GetItemSpec().withPrimaryKey("user_id", userId, "day", day);
//    System.out.println("Attempting to read the item...");
//    Item outcome = table.getItem(spec);
//    System.out.println("GetItem succeeded, ti: " + outcome.get("time_interval") + ", sc: " + outcome.get("step_count"));

    try {
      HashMap<String, String> nameMap = new HashMap<>();
      nameMap.put("#user_id", "user_id");
      nameMap.put("#day", "day");
      HashMap<String, Object> valueMap = new HashMap<>();
      valueMap.put(":user_id", userId);
      valueMap.put(":day", day);
      QuerySpec querySpec =
          new QuerySpec()
              .withKeyConditionExpression("#user_id = :user_id and #day = :day")
              .withNameMap(nameMap)
              .withValueMap(valueMap);
      ItemCollection<QueryOutcome> items = table.query(querySpec);
      int stepCount = 0;
      for (Item item : items) {
        stepCount += item.getInt("step_count");
      }
      System.out.println("Step count: " + stepCount);
    } catch (Exception e) {
      System.err.println("Unable to read item: " + userId + " " + day);
      System.err.println(e.getMessage());
      throw e;
    }
  }

  static void deleteTable(final DynamoDB dynamoDB) throws Exception {
    Table table = dynamoDB.getTable(TABLE_NAME);
    try {
      System.out.println("Attempting to delete table; please wait...");
      table.delete();
      table.waitForDelete();
      System.out.print("Success.");

    } catch (Exception e) {
      System.err.println("Unable to delete table: ");
      System.err.println(e.getMessage());
      throw e;
    }
  }

  public static void main(String[] args) throws Exception {
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
        .build();
//    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//        .withRegion(Regions.REGION)
//        .build();
    DynamoDB dynamoDB = new DynamoDB(client);
//    createTable(dynamoDB);
//    writeData(dynamoDB);
    getData(dynamoDB);
  }
}