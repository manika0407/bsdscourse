package com.project.bsds.bsdsassignment.db;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DynamoStepCounterDB implements IStepCounterDB {
  private static final String TABLE_NAME = "stepcounter_test";
  private static final long PROVISIONED_READ_THROUGHPUT = 10L;
  private static final long PROVISIONED_WRITE_THROUGHPUT = 10L;
  private static final boolean REMOTE_DB = true;
  private static final boolean RESET_TABLE = true;
  private static final int MAX_TS = 24;
  private static final int MAX_DAY = 99999;
  private static final int MAX_USER = 5000;

  private DynamoDB dynamoDB;
  private Table table;

  private HashMap<String, String> keyNameMap;

  private static DynamoStepCounterDB staticDBInstance;

  public static DynamoStepCounterDB getInstance() {
    if (staticDBInstance == null) {
      synchronized (DynamoStepCounterDB.class) {
        if (staticDBInstance == null) {
          try {
            staticDBInstance = new DynamoStepCounterDB();
          } catch (IOException e) {
            throw new RuntimeException("couldn't create db instance", e);
          }
        }
      }
    }
    return staticDBInstance;
  }

  private DynamoStepCounterDB() throws IOException {
    connect();
    checkOrCreateTable(RESET_TABLE);
    keyNameMap = new HashMap<>();
    keyNameMap.put("#user_id", "user_id");
    keyNameMap.put("#day_ts", "day_ts");
  }


  private void connect() throws IOException {
    AmazonDynamoDB client;
    if (REMOTE_DB) {
      // Note:- don't share this in prod
      BasicAWSCredentials awsCreds =
          new BasicAWSCredentials(
              "AKIAJPIXKI6C4EB7MHLQ", "puFcIbNwOspW499w+RW7CRChW+WcB5M6iDgqFJeb");
      client = AmazonDynamoDBClientBuilder.standard()
          .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
          .withRegion(Regions.US_WEST_2)
        .build();
    } else {
      client =
          AmazonDynamoDBClientBuilder.standard()
              .withEndpointConfiguration(
                  new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
              .build();
    }
    dynamoDB = new DynamoDB(client);
  }

  private void createTable() throws IOException {
    try {
      // create table
      table =
          dynamoDB.createTable(
              TABLE_NAME,
              Arrays.asList(
                  new KeySchemaElement("user_id", KeyType.HASH), // Partition
                  // key
                  new KeySchemaElement("day_ts", KeyType.RANGE)), // Sort key "day/ts"
              Arrays.asList(
                  new AttributeDefinition("user_id", ScalarAttributeType.N),
                  new AttributeDefinition("day_ts", ScalarAttributeType.S)),
              new ProvisionedThroughput(
                  PROVISIONED_READ_THROUGHPUT, PROVISIONED_WRITE_THROUGHPUT));
      table.waitForActive();
    } catch (Exception e) {
      throw new IOException("can't create table", e);
    }
  }

  @Override
  public void checkOrCreateTable(boolean resetTable) throws IOException {
    try {
      try {
        table = dynamoDB.getTable(TABLE_NAME);
        TableDescription tableDescription = table.describe();
        System.out.println("Table " + TABLE_NAME + " description: " + tableDescription.getItemCount());
      } catch (ResourceNotFoundException rnfe) {
        table = null;
        System.out.println("Table " + TABLE_NAME + " does not exist");
      }
      if (table != null && resetTable) {
        truncateTable();
        table = null;
      }
      if (table == null) {
        createTable();
      }
    } catch (Exception e) {
      throw new IOException("can't create table", e);
    }
  }

  private void truncateTable() throws IOException {
    try {
      table.delete();
      table.waitForDelete();
      table = null;
    } catch (Exception e) {
      throw new IOException("can't truncate table", e);
    }
  }

  private String getDayTsKey(int day, int timeInterval) {
    return String.format("%05d/%02d", day, timeInterval);
  }

  private String getDayStartKey(int day) {
    return getDayTsKey(day, 0);
  }


  private String getDayEndKey(int day) {
    return getDayTsKey(day, MAX_TS + 1); // last time interval + 1
  }

  @Override
  public void insert(int userId, int day, int timeInterval, int stepCount, UserInfo userInfo) throws IOException {
    try {
      Map<String, String> userInfoMap = new HashMap<>();
      if (userInfo != null) {
        userInfoMap.put("state", userInfo.getState().getAbbreviation());
        userInfoMap.put("gender", userInfo.getGender().name());
      }
      table.putItem(
          new Item()
              .withPrimaryKey("user_id", userId, "day_ts", getDayTsKey(day, timeInterval))
              .withInt("day", day)
              .withInt("time_interval", timeInterval)
              .withInt("step_count", stepCount)
              .withMap("user_info", userInfoMap));
    } catch (Exception e) {
      throw new IOException("can't insert step counts for user", e);
    }
  }

  @Override
  public int getStepCount(int userId, int day) throws IOException {
    try {
      HashMap<String, Object> valueMap = new HashMap<>();
      valueMap.put(":user_id", userId);
      valueMap.put(":day_ts1", getDayStartKey(day));
      valueMap.put(":day_ts2", getDayEndKey(day));
      QuerySpec querySpec =
          new QuerySpec()
              .withKeyConditionExpression("#user_id = :user_id and #day_ts between :day_ts1 and :day_ts2")
              .withNameMap(keyNameMap)
              .withValueMap(valueMap);
      ItemCollection<QueryOutcome> items = table.query(querySpec);
      int stepCount = 0;
      for (Item item : items) {
        stepCount += item.getInt("step_count");
      }
      return stepCount;
    } catch (Exception e) {
      throw new IOException("can't get step counts for user", e);
    }
  }

  @Override
  public List<Integer> getStepCountsInDays(int userId, int startDay, int numDays) throws IOException {
    int endDay = startDay + numDays - 1;
    try {
      HashMap<String, Object> valueMap = new HashMap<>();
      valueMap.put(":user_id", userId);
      valueMap.put(":day_ts1", getDayStartKey(startDay));
      valueMap.put(":day_ts2", getDayEndKey(endDay));
      QuerySpec querySpec =
          new QuerySpec()
              .withKeyConditionExpression("#user_id = :user_id and #day_ts between :day_ts1 and :day_ts2")
              .withNameMap(keyNameMap)
              .withValueMap(valueMap);
      ItemCollection<QueryOutcome> items = table.query(querySpec);
      List<Integer> daySteps = new ArrayList<>();
      int currentDay = startDay;
      int currentDaySteps = 0;

      int day, sc;
      for (Item item : items) {
        day = item.getInt("day");
        sc = item.getInt("step_count");
        if (currentDay == day) {
          currentDaySteps += sc;
        } else {
          if (currentDaySteps > 0) {
            daySteps.add(currentDaySteps);
          }
          currentDay = day;
          currentDaySteps = sc;
        }
      }
      if (currentDaySteps > 0) {
        daySteps.add(currentDaySteps);
      }
      return daySteps;
    } catch (Exception e) {
      throw new IOException("can't get step counts for user", e);
    }
  }

  @Override
  public int getCurrentDayStepCount(int userId) throws IOException {
    try {
      HashMap<String, Object> valueMap = new HashMap<>();
      valueMap.put(":user_id", userId);
      valueMap.put(":day_ts", getDayEndKey(MAX_DAY));
      QuerySpec querySpec =
          new QuerySpec()
              .withKeyConditionExpression(
                  "#user_id = :user_id and #day_ts <= :day_ts")
              .withNameMap(keyNameMap)
              .withValueMap(valueMap)
              .withScanIndexForward(false)
              .withMaxResultSize(MAX_TS + 1);
      ItemCollection<QueryOutcome> items = table.query(querySpec);
      int stepCount = 0;
      int lastDay = MAX_DAY;

      int day;
      for (Item item : items) {
        day = item.getInt("day");
        if (day != lastDay) {
          if (lastDay != MAX_DAY) {
            break;
          } else {
            lastDay = day;
          }
        }
        stepCount += item.getInt("step_count");
      }
      return stepCount;
    } catch (Exception e) {
      throw new IOException("can't get step counts for user", e);
    }
  }

  private int getStepsAroundGivenUserDay(int randUserId, int randDay, boolean previousUserDayEntry) throws IOException {
    try {
      HashMap<String, Object> valueMap = new HashMap<>();
      valueMap.put(":user_id", randUserId);
      valueMap.put(":day_ts", getDayEndKey(randDay));
      String comparator = previousUserDayEntry ? "<=" : ">=";
      QuerySpec querySpec =
          new QuerySpec()
              .withKeyConditionExpression(
                  "#user_id = :user_id and #day_ts " + comparator + " :day_ts")
              .withNameMap(keyNameMap)
              .withValueMap(valueMap)
              .withScanIndexForward(!previousUserDayEntry)
              .withMaxResultSize(MAX_TS + 1);
      ItemCollection<QueryOutcome> items = table.query(querySpec);
      int stepCount = 0;
      int lastDay = MAX_DAY;

      int day;
      for (Item item : items) {
        day = item.getInt("day");
        if (day != lastDay) {
          if (lastDay != MAX_DAY) {
            break;
          } else {
            lastDay = day;
          }
        }
        stepCount += item.getInt("step_count");
      }
      return stepCount;
    } catch (Exception e) {
      throw new IOException("can't get step counts for user", e);
    }
  }

  @Override
  public int getRandomUserDayStepCount() throws IOException {
    try {
      TableDescription tableDescription = table.describe();
      return tableDescription.getItemCount().intValue();
    } catch(Exception e) {
      throw new IOException("can't get random step user", e);
    }
//    ThreadLocalRandom tr = ThreadLocalRandom.current();
//    int randomDay = tr.nextInt(MAX_DAY + 1);
//    for (int randomUser = 0; randomUser < MAX_USER; randomUser++) {
//      int steps = getStepsAroundGivenUserDay(randomUser, randomDay, false);
//      if (steps != 0) {
//        return steps;
//      }
//      steps = getStepsAroundGivenUserDay(randomUser, randomDay, true);
//      if (steps != 0) {
//        return steps;
//      }
//    }
//    return -1;
  }


  public static void main(String[] args) throws IOException {
    DynamoStepCounterDB db = DynamoStepCounterDB.getInstance();
    db.checkOrCreateTable(true);
    db.insert(1, 1, 1, 500, new UserInfo(1, State.ALASKA, Gender.MALE));
    db.insert(1, 1, 2, 200, new UserInfo(1, State.ILLINOIS, Gender.FEMALE));
    db.insert(1, 2, 3, 300, new UserInfo(1, State.HAWAII, Gender.MALE));
    db.insert(1, 2, 4, 600, new UserInfo(1, State.WASHINGTON, Gender.FEMALE));
    System.out.println(db.getStepCount(1, 1));
    System.out.println(db.getCurrentDayStepCount(1));
    System.out.println(db.getStepCountsInDays(1, 1, 2));
    System.out.println(db.getRandomUserDayStepCount());
  }
}
