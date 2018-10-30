package com.project.bsds.bsdsassignment;

public enum ErrorCodes {
  OK(200),
  SERVER_ERROR(500),
  BAD_REQUEST(400);

  private int errorCode;

  public int getErrorCode() {
    return this.errorCode;
  }

  ErrorCodes(int errorCode) {
    this.errorCode = errorCode;
  }
}
