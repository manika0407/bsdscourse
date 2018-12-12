package com.project.bsds.bsdsassignment.db;

import java.io.IOException;
import java.util.List;

public interface IStepCounterDB {
  void insert(int userId, int day, int timeInterval, int stepCount, UserInfo userInfo) throws IOException;
  int getStepCount(int userId, int day) throws IOException;
  List<Integer> getStepCountsInDays(int userId, int startDay, int numDays)
      throws IOException;
  int getCurrentDayStepCount(int userId) throws IOException;
  int getRandomUserDayStepCount() throws IOException;
  void checkOrCreateTable(boolean resetTable) throws IOException;
}
