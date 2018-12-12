package com.project.bsds.bsdsassignment.message_queue;

import com.project.bsds.bsdsassignment.db.Gender;
import com.project.bsds.bsdsassignment.db.State;
import com.project.bsds.bsdsassignment.db.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;


public class UserStepEvent {
  private int userId;
  private int day;
  private int timeInterval;
  private int stepCount;
  private String state;
  private String gender;
  private String time;

  public UserStepEvent() {
    this(new UserInfo(-1, State.LAST, Gender.MALE), -1, -1, -1);
  }

  public UserStepEvent(UserInfo userInfo, int day, int timeInterval, int stepCount) {
    this.userId = userInfo.getUserId();
    this.day = day;
    this.timeInterval = timeInterval;
    this.stepCount = stepCount;
    this.state = userInfo.getState().getAbbreviation();
    this.gender = userInfo.getGender().name();
    DateTimeFormatter patternFormat = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        .appendTimeZoneOffset("Z", true, 2, 4)
        .toFormatter();
    this.time = DateTime.now(DateTimeZone.UTC).toString(patternFormat);
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public int getTimeInterval() {
    return timeInterval;
  }

  public void setTimeInterval(int timeInterval) {
    this.timeInterval = timeInterval;
  }

  public int getStepCount() {
    return stepCount;
  }

  public void setStepCount(int stepCount) {
    this.stepCount = stepCount;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  @Override
  public String toString() {
    return "UserStepEvent{" +
        "userId=" + userId +
        ", day=" + day +
        ", timeInterval=" + timeInterval +
        ", stepCount=" + stepCount +
        ", state='" + state + '\'' +
        ", gender='" + gender + '\'' +
        ", time='" + time + '\'' +
        '}';
  }
}
