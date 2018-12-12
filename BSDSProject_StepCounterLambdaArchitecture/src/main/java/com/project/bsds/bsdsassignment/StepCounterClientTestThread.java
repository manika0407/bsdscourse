package com.project.bsds.bsdsassignment;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.project.bsds.bsdsassignment.StepCounterObjects.*;

class TestPhaseRunInfo {
  TestPhaseRunInfo(TestPhase testPhase, CountDownLatch cd) {
    this.testPhase = testPhase;
    this.testPhaseFinishLatch = cd;
    this.startTimeInterval = getTestPhaseProp(testPhase).startTestInterval;
    this.endTimeInterval = getTestPhaseProp(testPhase).endTestInterval;
  }

  int getNumIterations(int numTests) {
    return numTests * (this.endTimeInterval - this.startTimeInterval + 1);
  }

  int startTimeInterval;
  int endTimeInterval;
  TestPhase testPhase;
  CountDownLatch testPhaseFinishLatch;
}

/** @author manika2211 */
public class StepCounterClientTestThread extends Thread {
  private final int threadId;
  private StepCounterClient sc;
  private final TestParam testParam;

  private AtomicBoolean threadFinished;

  private final StatsRecorder statsRecorder;
  private final StatsRecorderST singleThreadStatRecorder;

  private final CyclicBarrier startBarrier;

  private final Stopwatch threadWatch;

  List<TestPhaseRunInfo> testPhaseRunInfos;

  private static final int CLIENT_REFRESH_ITERATIONS = 10;

  public StepCounterClientTestThread(
      int threadId,
      TestParam testParam,
      StatsRecorder statsRecorder,
      StatsRecorderST singleThreadStatRecorder,
      List<TestPhaseRunInfo> testPhaseRunInfos,
      StepCounterClient sc,
      CyclicBarrier startBarrier) {
    this.threadId = threadId;
    this.testParam = testParam;
    this.statsRecorder = statsRecorder;
    this.singleThreadStatRecorder = singleThreadStatRecorder;
    // getting system time is expensive
    this.threadWatch = Stopwatch.createStarted();
    this.testPhaseRunInfos = testPhaseRunInfos;
    this.sc = sc;
    this.startBarrier = startBarrier;
    this.threadFinished = new AtomicBoolean(false);
  }

  StepCounterClientTestThread(
      int threadId, TestParam testParam, TestPhase testPhase, StatsRecorder statsRecorder) {
    this(
        threadId,
        testParam,
        statsRecorder,
        null,
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(new TestPhaseRunInfo(testPhase, null));
          }
        },
        createClient(testParam),
        null);
  }

  StepCounterClientTestThread(
      int threadId,
      TestParam testParam,
      TestPhase testPhase,
      StatsRecorder statsRecorder,
      StatsRecorderST singleThreadStatRecorder) {
    this(
        threadId,
        testParam,
        statsRecorder,
        singleThreadStatRecorder,
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(new TestPhaseRunInfo(testPhase, null));
          }
        },
        createClient(testParam),
        null);
  }

  void addStat(long timeTakenMs, boolean succeeded) {
    if (this.statsRecorder != null) {
      this.statsRecorder.addValue((int) timeTakenMs, succeeded);
    }
    if (this.singleThreadStatRecorder != null) {
      this.singleThreadStatRecorder.addValue(
          threadWatch.elapsed(TimeUnit.MILLISECONDS) /* relative from start time ms */,
          (int) timeTakenMs,
          succeeded);
    }
  }

  @Override
  public void run() {
    Stopwatch requestTimer = Stopwatch.createUnstarted();
    int day = testParam.day;
    int userId1, userId2, userId3;
    int timeInterval1, timeInterval2, timeInterval3;
    int stepCount1, stepCount2, stepCount3;

    if (this.startBarrier != null) {
      try {
        this.startBarrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        System.exit(1);
      }
    }
    //    System.out.println("Starting thread : " + threadId);

    int numIterationsOnSameClient = 0;
    ThreadLocalRandom tr = ThreadLocalRandom.current();
    for (TestPhaseRunInfo testPhaseRunInfo : testPhaseRunInfos) {
      for (int j = 0; j < testPhaseRunInfo.getNumIterations(testParam.numTests); j++) {
        // Generate 3 random users, time intervals, step counts
        userId1 = tr.nextInt(testParam.numUsers) + 1;
        userId2 = tr.nextInt(testParam.numUsers) + 1;
        userId3 = tr.nextInt(testParam.numUsers) + 1;

        timeInterval1 =
            tr.nextInt(testPhaseRunInfo.startTimeInterval, testPhaseRunInfo.endTimeInterval + 1);
        timeInterval2 =
            tr.nextInt(testPhaseRunInfo.startTimeInterval, testPhaseRunInfo.endTimeInterval + 1);
        timeInterval3 =
            tr.nextInt(testPhaseRunInfo.startTimeInterval, testPhaseRunInfo.endTimeInterval + 1);

        stepCount1 = tr.nextInt(5001);
        stepCount2 = tr.nextInt(5001);
        stepCount3 = tr.nextInt(5001);

        // check if client needs to be refreshed
        if (numIterationsOnSameClient >= (tr.nextInt(CLIENT_REFRESH_ITERATIONS) + 2)) {
          sc = createClient(testParam);
          numIterationsOnSameClient = 0;
        }
        numIterationsOnSameClient++;

        // POST /userID1/day/timeInterval1/stepCount1
        // POST /userID2/day/timeInterval2l/stepCount2
        // GET /current/userID1
        // GET/single/userID2/day
        // POST /userID3/day/timeInterval3/stepCount3

        /* ================= POST Request User 1 =================== */
        boolean succeeded = false;
        requestTimer.reset();
        requestTimer.start();
        try {
          sc.uploadStepsData(userId1, day, timeInterval1, stepCount1);
          succeeded = true;
        } catch (RuntimeException e) {
          // e.printStackTrace();
        }
        requestTimer.stop();
        addStat(requestTimer.elapsed(TimeUnit.MILLISECONDS), succeeded);

        /* ================= Post Request User 2 =================== */

        succeeded = false;
        requestTimer.reset();
        requestTimer.start();
        try {
          sc.uploadStepsData(userId2, day, timeInterval2, stepCount2);
          succeeded = true;
        } catch (RuntimeException e) {
          // e.printStackTrace();
        }
        requestTimer.stop();
        addStat(requestTimer.elapsed(TimeUnit.MILLISECONDS), succeeded);

        /* ================= Get Current Day Request User 1 =================== */

        succeeded = false;
        requestTimer.reset();
        requestTimer.start();
        try {
          sc.getCurrentStepsForUser(userId1);
          succeeded = true;
        } catch (RuntimeException e) {
          // e.printStackTrace();
        }
        requestTimer.stop();
        addStat(requestTimer.elapsed(TimeUnit.MILLISECONDS), succeeded);

        /* ================= Get DAY Request User 2 =================== */

        succeeded = false;
        requestTimer.reset();
        requestTimer.start();
        try {
          sc.getSingleDayStepsForUser(userId2, day);
          succeeded = true;
        } catch (RuntimeException e) {
          // e.printStackTrace();
        }
        requestTimer.stop();
        addStat(requestTimer.elapsed(TimeUnit.MILLISECONDS), succeeded);

        /* ================= Post Request User 3 =================== */

        succeeded = false;
        requestTimer.reset();
        requestTimer.start();
        try {
          sc.uploadStepsData(userId3, day, timeInterval3, stepCount3);
          succeeded = true;
        } catch (RuntimeException e) {
          // e.printStackTrace();
        }
        requestTimer.stop();
        addStat(requestTimer.elapsed(TimeUnit.MILLISECONDS), succeeded);
      }
      if (testPhaseRunInfo.testPhaseFinishLatch != null) {
        // finish signalling test phase end for this thread
        testPhaseRunInfo.testPhaseFinishLatch.countDown();
      }
    }
    this.threadWatch.stop();
    this.threadFinished.set(true);
    try {
      this.sc.close();
    } catch (IOException e) {
      // ignore
    }
    //    System.out.println("Finished thread : " + threadId);
  }
}
