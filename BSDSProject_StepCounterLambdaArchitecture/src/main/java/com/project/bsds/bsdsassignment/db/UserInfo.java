package com.project.bsds.bsdsassignment.db;

public class UserInfo {
  private int userId;
  private State state;
  private Gender gender;

  public UserInfo(int userId, State state, Gender gender) {
    this.userId = userId;
    this.state = state;
    this.gender = gender;
  }

  public int getUserId() {
    return userId;
  }

  public State getState() {
    return state;
  }

  public Gender getGender() {
    return gender;
  }

  @Override
  public String toString() {
    return "UserInfo{" +
        "userId=" + userId +
        ", state=" + state +
        ", gender=" + gender +
        '}';
  }
}

