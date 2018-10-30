package com.project.bsds.bsdsassignment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StepCounterDB {
  private static final boolean REMOTE_DB = true; // use remote rds db or local mysql db
  private static final String DB_ADDRESS =
      REMOTE_DB
          ? "bsdsassignment2-stepcounter-db.cumfxxz0jzwo.us-west-2.rds.amazonaws.com"
          : "localhost";

  private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
  private static final String DB_NAME = "bsdsassignment2";
  private static final String TABLE_NAME = "stepcounter";
  private static final String DB_USERNAME = "manika2211"; // TODO this shouldn't be shared
  private static final String DB_PASSWORD = "manika2211"; // but ok for testing purposes
  private static final Integer DB_PORT = 3306;
  private static final Integer CONN_POOL_SIZE = 50; // keep this pool size for 5-6 ec2 instances (~300)
  private static final Integer ST_CACHE_SIZE = 1000;
  private static final boolean RESET_TABLE = false;

  private DataSource dataSource;

  private static StepCounterDB staticDBInstance;

  public static StepCounterDB getInstance() {
    if (staticDBInstance == null) {
      synchronized (StepCounterDB.class) {
        if (staticDBInstance == null) {
          try {
            staticDBInstance = new StepCounterDB();
          } catch (IOException e) {
            throw new RuntimeException("couldn't create db instance", e);
          }
        }
      }
    }
    return staticDBInstance;
  }

  private StepCounterDB() throws IOException {
    connect();
    checkOrCreateTable(RESET_TABLE);
  }

  private DataSource getDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://" + DB_ADDRESS + ":" + DB_PORT + "/" + DB_NAME);
    config.setUsername(DB_USERNAME);
    config.setPassword(DB_PASSWORD);

    config.setMaximumPoolSize(CONN_POOL_SIZE);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", String.valueOf(ST_CACHE_SIZE));
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "10240");
    config.addDataSourceProperty("useSSL", "false");
    config.setLeakDetectionThreshold(2000);
    return new HikariDataSource(config);
  }

  private void connect() throws IOException {
    try {
      //            Context initContext = new InitialContext();
      //            Context envContext  = (Context)initContext.lookup("java:/comp/env");
      //            DataSource ds = (DataSource)envContext.lookup("jdbc/stepcounterdb");
      //            connection = ds.getConnection();
      //      connection =
      //          DriverManager.getConnection(
      //              "jdbc:mysql://" + DB_ADDRESS + ":" + DB_PORT + "/" + DB_NAME,
      //              DB_USERNAME,
      //              DB_PASSWORD);
      Class.forName(DB_DRIVER).newInstance();
      dataSource = getDataSource();
      System.out.println("Successfully connected to step counter db.");
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      System.out.println("Connection Failed!:\n" + e.getMessage());
      throw new IOException(e);
    }
  }

  private void truncateTable() throws IOException {
    String sqlQuery = "truncate table " + TABLE_NAME + ";";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
      statement.executeUpdate();
    } catch (SQLException se) {
      throw new IOException("can't create table", se);
    }
  }

  public void checkOrCreateTable(boolean resetTable) throws IOException {
    String sqlQuery =
        "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME
            + "(id INT AUTO_INCREMENT, "
            + "user_id INT NOT NULL, "
            + "day INT NOT NULL, "
            + "time_interval INT NOT NULL, "
            + "step_count INT NOT NULL, "
            + "PRIMARY KEY(id), "
            + "INDEX user_day_index (user_id, day)"
            + ")ENGINE=INNODB;";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
      int row = statement.executeUpdate();
      if (row != 0) {
        throw new IllegalStateException("no row expected.");
      }
      if (resetTable) {
        truncateTable();
      }
    } catch (SQLException se) {
      throw new IOException("can't create table", se);
    }
  }

  public void insert(int userId, int day, int timeInterval, int stepCount) throws IOException {
    String sqlQuery =
        "INSERT INTO "
            + TABLE_NAME
            + " (user_id, day, time_interval, step_count) VALUES ("
            + userId
            + ","
            + day
            + ","
            + timeInterval
            + ","
            + stepCount
            + ");";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
      int row = statement.executeUpdate();
      if (row != 1) {
        throw new IllegalStateException("only 1 row expected.");
      }
    } catch (SQLException se) {
      throw new IOException("can't insert step counts for user", se);
    }
  }

  public int getStepCount(int userId, int day) throws IOException {
    String sqlQuery =
        "SELECT SUM(step_count) as steps from "
            + TABLE_NAME
            + " where user_id = "
            + userId
            + " and day = "
            + day;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet rs = statement.executeQuery()) {
      int steps = -1;
      while (rs.next()) {
        if (steps >= 0) {
          throw new IllegalStateException("only 1 row expected.");
        }
        steps = rs.getInt("steps");
      }
      return steps;
    } catch (SQLException se) {
      throw new IOException("can't get step counts for user", se);
    }
  }

  public int getCurrentDayStepCount(int userId) throws IOException {
    String sqlQuery =
        "SELECT SUM(step_count) as steps from "
            + TABLE_NAME
            + " where user_id = "
            + userId
            + " and day = (select MAX(day) from "
            + TABLE_NAME
            + " where user_id = "
            + userId
            + ");";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet rs = statement.executeQuery()) {
      int steps = -1;
      while (rs.next()) {
        if (steps >= 0) {
          throw new IllegalStateException("only 1 row expected.");
        }
        steps = rs.getInt("steps");
      }
      return steps;
    } catch (SQLException se) {
      throw new IOException("can't get step counts for user", se);
    }
  }

  public int getRandomUserDayStepCount() throws IOException {
    // check by looking at random user step count for current day
    String sqlQuery =
        "select sum(S1.step_count) as steps from "
            + TABLE_NAME
            + " S1, (SELECT * FROM "
            + TABLE_NAME
            + " S where id >= rand() * (select max(id) from "
            + TABLE_NAME
            + ") limit 1) S2  where  S1.user_id = S2.user_id and S1.day = S2.day;";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet rs = statement.executeQuery()) {
      int steps = -1;
      while (rs.next()) {
        if (steps >= 0) {
          throw new IllegalStateException("only 1 row expected.");
        }
        steps = rs.getInt("steps");
      }
      return steps;
    } catch (SQLException se) {
      throw new IOException("can't get step counts for user", se);
    }
  }

  public List<Integer> getStepCountsInDays(int userId, int startDay, int numDays)
      throws IOException {
    int endDay = startDay + numDays - 1;
    String sqlQuery =
        "SELECT day, SUM(step_count) as steps from "
            + TABLE_NAME
            + " where user_id = "
            + userId
            + " and day >= "
            + startDay
            + " and day <= "
            + endDay
            + " group by day order by day ASC;";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet rs = statement.executeQuery()) {
      List<Integer> daySteps = new ArrayList<>();
      while (rs.next()) {
        daySteps.add(rs.getInt("steps"));
      }
      return daySteps;
    } catch (SQLException se) {
      throw new IOException("can't get step counts for user", se);
    }
  }

  public static void main(String[] args) throws IOException {
    StepCounterDB db = StepCounterDB.getInstance();
    db.truncateTable();
    db.insert(1, 1, 1, 500);
    db.insert(1, 1, 2, 200);
    db.insert(1, 2, 3, 300);
    db.insert(1, 2, 4, 400);
    System.out.println(db.getStepCount(1, 1));
    System.out.println(db.getCurrentDayStepCount(1));
    System.out.println(db.getStepCountsInDays(1, 1, 2));
    System.out.println(db.getRandomUserDayStepCount());
  }
}
