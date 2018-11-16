package com.project.bsds.bsdsassignment.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.project.bsds.bsdsassignment.ErrorCodes;
import com.project.bsds.bsdsassignment.StepCounterDB;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepCounterLambda implements RequestHandler<StepCounterLambdaRequest, StepCounterLambdaResponse> {

  private static StepCounterDB db;

  private static final String URL_PREFIX = "/bsdsassignment2-webapp/stepcounter";

  enum QueryType {
    NONE,
    INSERT,
    CURRENT,
    SINGLE,
    RANGE,
    CLEAR;
  }
  Map<QueryType, Pattern> queryTypePatternMap;

  public StepCounterLambda() {
    queryTypePatternMap = new HashMap<>();
    queryTypePatternMap.put(QueryType.INSERT, Pattern.compile(URL_PREFIX + "/([1-9][0-9]*)/([1-9][0-9]*)/([0-9]|1[0-9]|2[0-3])/(\\d+)"));
    queryTypePatternMap.put(QueryType.CURRENT, Pattern.compile(URL_PREFIX + "/current/([1-9][0-9]*)"));
    queryTypePatternMap.put(QueryType.SINGLE, Pattern.compile(URL_PREFIX + "/single/([1-9][0-9]*)/([1-9][0-9]*)"));
    queryTypePatternMap.put(QueryType.RANGE, Pattern.compile(URL_PREFIX + "/range/([1-9][0-9]*)/([1-9][0-9]*)/(\\d+)"));
    queryTypePatternMap.put(QueryType.CLEAR, Pattern.compile(URL_PREFIX + "/clear"));
    db = StepCounterDB.getInstance();
  }
  
  static class QueryInfo {
    QueryType queryType = QueryType.NONE;
    List<String> queryPathParts = new ArrayList<>();
  }

  QueryInfo getQueryInfo(String url) {
    QueryInfo queryInfo = new QueryInfo();
    for (Map.Entry<QueryType, Pattern> kv : queryTypePatternMap.entrySet()) {
      Matcher matcher = kv.getValue().matcher(url);
      if (matcher.find()) {
        queryInfo.queryType = kv.getKey();
        for (int matchIdx = 1; matchIdx <= matcher.groupCount(); matchIdx++) {
          queryInfo.queryPathParts.add(matcher.group(matchIdx));
        }
        break;
      }
    }
    return queryInfo;
  }
  
  @Override
  public StepCounterLambdaResponse handleRequest(StepCounterLambdaRequest input, Context context) {
    String url = input.getPath();
    QueryInfo queryInfo = getQueryInfo(url);
    if (queryInfo.queryType == QueryType.NONE) {
      return new StepCounterLambdaResponse().builder().withStatusCode(ErrorCodes.BAD_REQUEST.getErrorCode()).withBody("invalid url " + url).build();
    }

    ErrorCodes errorCode;
    String resultMsg;
    try {
      switch (queryInfo.queryType) {
        case INSERT: {
          int userId = Integer.parseInt(queryInfo.queryPathParts.get(0));
          int day = Integer.parseInt(queryInfo.queryPathParts.get(1));
          int timeInterval = Integer.parseInt(queryInfo.queryPathParts.get(2));
          int stepCount = Integer.parseInt(queryInfo.queryPathParts.get(3));
          db.insert(userId, day, timeInterval, stepCount);
          errorCode = ErrorCodes.OK;
          resultMsg = "Successfully uploaded";
          break;
        }
        case CURRENT: {
          int userId = Integer.parseInt(queryInfo.queryPathParts.get(0));
          int steps = db.getCurrentDayStepCount(userId);
          errorCode = ErrorCodes.OK;
          resultMsg = String.valueOf(steps);
          break;
        }
        case SINGLE: {
          int userId = Integer.parseInt(queryInfo.queryPathParts.get(0));
          int day = Integer.parseInt(queryInfo.queryPathParts.get(1));
          int steps = db.getStepCount(userId, day);
          errorCode = ErrorCodes.OK;
          resultMsg = String.valueOf(steps);
          break;
        }
        case RANGE: {
          int userId = Integer.parseInt(queryInfo.queryPathParts.get(0));
          int startDay = Integer.parseInt(queryInfo.queryPathParts.get(1));
          int numDays = Integer.parseInt(queryInfo.queryPathParts.get(2));
          List<Integer> stepsPerDay = db.getStepCountsInDays(userId, startDay, numDays);
          errorCode = ErrorCodes.OK;
          resultMsg = StringUtils.join(stepsPerDay, ",");
          break;
        }
        case CLEAR: {
          db.checkOrCreateTable(true /* reset */);
          errorCode = ErrorCodes.OK;
          resultMsg = "successfully truncated table";
          break;
        }
        default: {
          throw new IllegalStateException("not expected.");
        }
      }
    } catch (IOException e) {
      errorCode = ErrorCodes.SERVER_ERROR;
      resultMsg = "Failed to perform query: " + queryInfo.queryType.name() + " because of error: " + e.toString();
    }
    return new StepCounterLambdaResponse().builder().withStatusCode(errorCode.getErrorCode()).withBody(resultMsg).build();
  }

  public static void main(String[] args) {
    StepCounterLambda s = new StepCounterLambda();
    QueryInfo queryInfo = s.getQueryInfo(URL_PREFIX + "/4000/1/2/200");
    System.out.println("type = " + queryInfo.queryType.name() + " params = " + StringUtils.join(queryInfo.queryPathParts, ","));

    queryInfo = s.getQueryInfo(URL_PREFIX + "/current/1");
    System.out.println("type = " + queryInfo.queryType.name() + " params = " + StringUtils.join(queryInfo.queryPathParts, ","));

    queryInfo = s.getQueryInfo(URL_PREFIX + "/single/2000/1");
    System.out.println("type = " + queryInfo.queryType.name() + " params = " + StringUtils.join(queryInfo.queryPathParts, ","));

    queryInfo = s.getQueryInfo(URL_PREFIX + "/range/2000/1/10");
    System.out.println("type = " + queryInfo.queryType.name() + " params = " + StringUtils.join(queryInfo.queryPathParts, ","));

    queryInfo = s.getQueryInfo(URL_PREFIX + "/clear");
    System.out.println("type = " + queryInfo.queryType.name() + " params = " + StringUtils.join(queryInfo.queryPathParts, ","));
  }
}
