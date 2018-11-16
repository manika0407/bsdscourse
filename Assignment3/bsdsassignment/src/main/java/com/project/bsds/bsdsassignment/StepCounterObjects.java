package com.project.bsds.bsdsassignment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StepCounterObjects {
  public static boolean DEBUG_LOG_ENABLED = false;

  enum TestPhase {
    WARMUP(0),
    LOADING(1),
    PEAK(2),
    COOLDOWN(3);

    private int testPhase;

    public int getTestPhase() {
      return this.testPhase;
    }

    TestPhase(int testPhase) {
      this.testPhase = testPhase;
    }
  }

  static class TestPhaseProp {
    TestPhaseProp(double t, int s, int e) {
      this.pctThreads = t;
      this.startTestInterval = s;
      this.endTestInterval = e;
      if (this.pctThreads < 0.0
          || this.pctThreads > 1.0
          || this.startTestInterval < 0
          || this.endTestInterval > 23
          || this.endTestInterval < this.startTestInterval) {
        throw new IllegalArgumentException("not allowed");
      }
    }

    int getPhaseNumThreads(int maxThreads) {
      return (int) (pctThreads * maxThreads);
    }

    int getPhaseLength() {
      return endTestInterval - startTestInterval + 1;
    }

    double pctThreads;
    int startTestInterval;
    int endTestInterval;
  }

  private static final Map<TestPhase, TestPhaseProp> testPhaseThreadPctMap;

  static {
    Map<TestPhase, TestPhaseProp> temp = new HashMap<>();
    temp.put(TestPhase.WARMUP, new TestPhaseProp(0.1, 0, 2));
    temp.put(TestPhase.LOADING, new TestPhaseProp(0.5, 3, 7));
    temp.put(TestPhase.PEAK, new TestPhaseProp(1.0, 8, 18));
    temp.put(TestPhase.COOLDOWN, new TestPhaseProp(0.25, 19, 23));
    testPhaseThreadPctMap = Collections.unmodifiableMap(temp);
  }

  static TestPhaseProp getTestPhaseProp(final TestPhase testPhase) {
    return testPhaseThreadPctMap.get(testPhase);
  }

  static TestPhaseProp getTestPhaseProp(int testPhase) {
    return testPhaseThreadPctMap.get(TestPhase.values()[testPhase]);
  }

  static class TestParam {
    TestParam(
        String serverIp,
        int serverPort,
        String serverUri,
        int maxThreads,
        int numTests,
        int day,
        int numUsers) {
      this.serverIp = serverIp;
      this.serverPort = serverPort;
      this.serverUri = serverUri;
      this.maxThreads = maxThreads;
      this.day = day;
      this.numUsers = numUsers;
      this.numTests = numTests;
      //      this.startTimeInterval = startTimeInterval;
      //      this.endTimeInterval = endTimeInterval;
      //      this.numIterations = numTests * (endTimeInterval - startTimeInterval + 1);
    }

    String serverIp;
    int serverPort;
    String serverUri;
    int maxThreads;
    int day;
    int numUsers;
    int numTests;
    //    int numIterations;
    //    int startTimeInterval;
    //    int endTimeInterval;
  }

  static StepCounterClient createClient(TestParam testParam) {
    return new StepCounterClient(testParam.serverIp, testParam.serverPort, testParam.serverUri);
  }


  static class TestPhaseStats {
    int numTestIterations = 0;
    int numTotalRequests = 0;
    int numSuccessfullRequests = 0;
    long p99LatencyMs = 0;
    long p95LatencyMs = 0;
    long totalTimeMs = 0;
    long[] requestsProcessedPerSecond;
    long[] requestsSentPerSecond;
  }

  static TestPhaseStats aggregateStats(TestPhaseStats... tsArr) {
    TestPhaseStats result = new TestPhaseStats();
    int s1 = 0, s2 = 0;
    for (TestPhaseStats ts : tsArr) {
      s1 += ts.requestsProcessedPerSecond.length;
      s2 += ts.requestsSentPerSecond.length;
    }
    result.requestsProcessedPerSecond = new long[s1];
    result.requestsSentPerSecond = new long[s2];

    s1 = 0;
    s2 = 0;
    for (TestPhaseStats ts : tsArr) {
      result.numTestIterations += ts.numTestIterations;
      result.numTotalRequests += ts.numTotalRequests;
      result.numSuccessfullRequests += ts.numSuccessfullRequests;
      result.totalTimeMs += ts.totalTimeMs;
      if (result.p95LatencyMs == 0) {
        result.p95LatencyMs = ts.p95LatencyMs;
      } else {
        result.p95LatencyMs = (result.p95LatencyMs + ts.p95LatencyMs) / 2;
      }
      if (result.p99LatencyMs == 0) {
        result.p99LatencyMs = ts.p99LatencyMs;
      } else {
        result.p99LatencyMs = (result.p99LatencyMs + ts.p99LatencyMs) / 2;
      }
      System.arraycopy(
          ts.requestsProcessedPerSecond,
          0,
          result.requestsProcessedPerSecond,
          s1,
          ts.requestsProcessedPerSecond.length);
      System.arraycopy(
          ts.requestsSentPerSecond,
          0,
          result.requestsSentPerSecond,
          s2,
          ts.requestsSentPerSecond.length);
      s1 = ts.requestsProcessedPerSecond.length;
      s2 = ts.requestsSentPerSecond.length;
    }
    return result;
  }

  static TestPhaseStats toTestPhaseStats(List<StatsRecorderST.BucketStats> bucketsStats) {
    TestPhaseStats result = new TestPhaseStats();
    result.requestsSentPerSecond = new long[bucketsStats.size()];
    result.requestsProcessedPerSecond = new long[bucketsStats.size()];
    for (int i = 0; i < bucketsStats.size(); i++) {
      StatsRecorderST.BucketStats bs = bucketsStats.get(i);
      result.numTotalRequests += bs.totalRequests;
      result.numSuccessfullRequests += bs.successfullRequests;
      if (result.p95LatencyMs == 0) {
        result.p95LatencyMs = bs.p95LatencyMs;
      } else {
        result.p95LatencyMs = (result.p95LatencyMs + bs.p95LatencyMs) / 2;
      }
      if (result.p99LatencyMs == 0) {
        result.p99LatencyMs = bs.p99LatencyMs;
      } else {
        result.p99LatencyMs = (result.p99LatencyMs + bs.p99LatencyMs) / 2;
      }
      result.requestsProcessedPerSecond[i] = bs.successfullRequests;
      result.requestsSentPerSecond[i] = bs.totalRequests;
    }
    return result;
  }
}
