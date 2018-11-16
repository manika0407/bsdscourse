package com.project.bsds.bsdsassignment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static com.project.bsds.bsdsassignment.StepCounterObjects.*;

public class StepCounterThreadManager {
  private final TestParam testParam;
  private StatsRecorder statsRecorder;
  private StepCounterClient stepCounterSharedClient;
  private static final boolean USE_SINGLE_THREAD_STAT_RECORDER =
      true; // using per thread stat recorder to avoid synchronization overhead
  private static final boolean USE_SINGLE_SHARED_CLIENT =
      false; // for sharing same client across all threads

  public StepCounterThreadManager(TestParam testParam) {
    this.testParam = testParam;
    this.statsRecorder = USE_SINGLE_THREAD_STAT_RECORDER ? null : new StatsRecorder();
    this.stepCounterSharedClient =
        USE_SINGLE_SHARED_CLIENT
            ? new StepCounterClient(testParam.serverIp, testParam.serverPort, testParam.serverUri)
            : null;
  }

  StepCounterClient getClient() {
    if (USE_SINGLE_SHARED_CLIENT) {
      return this.stepCounterSharedClient;
    } else {
      return createClient(this.testParam);
    }
  }

  TestPhaseStats start() throws BrokenBarrierException, InterruptedException {
    List<Thread> threads = new ArrayList<>();
    TestPhaseProp warmupPhaseProp = getTestPhaseProp(TestPhase.WARMUP);
    TestPhaseProp loadingPhaseProp = getTestPhaseProp(TestPhase.LOADING);
    TestPhaseProp peakPhaseProp = getTestPhaseProp(TestPhase.PEAK);
    TestPhaseProp cooldownPhaseProp = getTestPhaseProp(TestPhase.COOLDOWN);

    int numWarmupThreads = warmupPhaseProp.getPhaseNumThreads(testParam.maxThreads);
    int numLoadingThreads = loadingPhaseProp.getPhaseNumThreads(testParam.maxThreads);
    int numPeakThreads = peakPhaseProp.getPhaseNumThreads(testParam.maxThreads);
    int numCooldownThreads = cooldownPhaseProp.getPhaseNumThreads(testParam.maxThreads);

    CountDownLatch warmupPhaseCompletionLatch = new CountDownLatch(numWarmupThreads);
    int warmupPhaseStartThreads = numWarmupThreads;
    CyclicBarrier warmupPhaseBarrier = new CyclicBarrier(warmupPhaseStartThreads + 1);

    CountDownLatch loadingPhaseCompletionLatch = new CountDownLatch(numLoadingThreads);
    int loadingPhaseStartThreads1 = numCooldownThreads - numWarmupThreads;
    int loadingPhaseStartThreads2 = numLoadingThreads - numCooldownThreads;
    CyclicBarrier loadingPhaseBarrier =
        new CyclicBarrier(loadingPhaseStartThreads1 + loadingPhaseStartThreads2 + 1);

    CountDownLatch peakPhaseCompletionLatch = new CountDownLatch(numPeakThreads);
    int peakPhaseStartThreads = numPeakThreads - numLoadingThreads;
    CyclicBarrier peakPhaseBarrier = new CyclicBarrier(peakPhaseStartThreads + 1);

    CountDownLatch cooldownPhaseCompletionLatch = new CountDownLatch(numCooldownThreads);

    TestPhaseRunInfo warmupPhaseInfo =
        new TestPhaseRunInfo(TestPhase.WARMUP, warmupPhaseCompletionLatch);
    TestPhaseRunInfo loadingPhaseInfo =
        new TestPhaseRunInfo(TestPhase.LOADING, loadingPhaseCompletionLatch);
    TestPhaseRunInfo peakPhaseInfo = new TestPhaseRunInfo(TestPhase.PEAK, peakPhaseCompletionLatch);
    TestPhaseRunInfo cooldownPhaseInfo =
        new TestPhaseRunInfo(TestPhase.COOLDOWN, cooldownPhaseCompletionLatch);

    int threadId = 0;
    StatsRecorderST[] statsRecorderSTList = new StatsRecorderST[testParam.maxThreads];
    if (USE_SINGLE_THREAD_STAT_RECORDER) {
      for (int i = 0; i < testParam.maxThreads; i++) {
        statsRecorderSTList[i] = new StatsRecorderST(0); // timing start from 0
      }
    }

    // 10% run for all phases
    List<TestPhaseRunInfo> testPhaseRunInfos =
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(warmupPhaseInfo);
            add(loadingPhaseInfo);
            add(peakPhaseInfo);
            add(cooldownPhaseInfo);
          }
        };
    for (int i = 0; i < warmupPhaseStartThreads; i++) {
      Thread t =
          new StepCounterClientTestThread(
              threadId,
              testParam,
              statsRecorder,
              statsRecorderSTList[threadId],
              testPhaseRunInfos,
              getClient(),
              warmupPhaseBarrier);
      t.start();
      threads.add(t);
      threadId++;
    }

    // + 15% run for loading, peak and cooldown phases
    testPhaseRunInfos =
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(loadingPhaseInfo);
            add(peakPhaseInfo);
            add(cooldownPhaseInfo);
          }
        };
    for (int i = 0; i < loadingPhaseStartThreads1; i++) {
      Thread t =
          new StepCounterClientTestThread(
              threadId,
              testParam,
              statsRecorder,
              statsRecorderSTList[threadId],
              testPhaseRunInfos,
              getClient(),
              loadingPhaseBarrier);
      t.start();
      threads.add(t);
      threadId++;
    }

    // +25% run for loading and peak phase
    testPhaseRunInfos =
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(loadingPhaseInfo);
            add(peakPhaseInfo);
          }
        };
    for (int i = 0; i < loadingPhaseStartThreads2; i++) {
      Thread t =
          new StepCounterClientTestThread(
              threadId,
              testParam,
              statsRecorder,
              statsRecorderSTList[threadId],
              testPhaseRunInfos,
              getClient(),
              loadingPhaseBarrier);
      t.start();
      threads.add(t);
      threadId++;
    }

    // + 50% run for peak phase only
    testPhaseRunInfos =
        new ArrayList<TestPhaseRunInfo>() {
          {
            add(peakPhaseInfo);
          }
        };
    for (int i = 0; i < peakPhaseStartThreads; i++) {
      Thread t =
          new StepCounterClientTestThread(
              threadId,
              testParam,
              statsRecorder,
              statsRecorderSTList[threadId],
              testPhaseRunInfos,
              getClient(),
              peakPhaseBarrier);
      t.start();
      threads.add(t);
      threadId++;
    }
    if (threadId != testParam.maxThreads) {
      throw new IllegalStateException("not expected.");
    }

    long startTime = System.currentTimeMillis();
    System.out.println("Client starting.... Time: " + startTime);

    long testPhaseStartTime, testPhaseEndTime;

    // start warmup phase
    warmupPhaseBarrier.await();
    testPhaseStartTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.WARMUP.name() + ": All threads(" + numWarmupThreads + ") running....");
    // wait for threads in warmup phase to finish its iterations
    warmupPhaseCompletionLatch.await();
    testPhaseEndTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.WARMUP.name()
            + " complete: Time "
            + (testPhaseEndTime - testPhaseStartTime) / 1000.0
            + " seconds");

    // now signal load phase threads to start
    loadingPhaseBarrier.await();
    testPhaseStartTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.LOADING.name() + ": All threads(" + numLoadingThreads + ") running....");
    // wait for load phase threads to finish its iterations
    loadingPhaseCompletionLatch.await();
    testPhaseEndTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.LOADING.name()
            + " complete: Time "
            + (testPhaseEndTime - testPhaseStartTime) / 1000.0
            + " seconds");

    // start peak phase threads
    peakPhaseBarrier.await();
    testPhaseStartTime = System.currentTimeMillis();
    System.out.println(TestPhase.PEAK.name() + ": All threads(" + numPeakThreads + ") running....");
    // wait for peak phase threads to finish its iterations
    peakPhaseCompletionLatch.await();
    testPhaseEndTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.PEAK.name()
            + " complete: Time "
            + (testPhaseEndTime - testPhaseStartTime) / 1000.0
            + " seconds");

    // no new threads have to be started for cooldown so just wait for its completion
    testPhaseStartTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.COOLDOWN.name() + ": All threads(" + numCooldownThreads + ") running....");
    cooldownPhaseCompletionLatch.await();
    testPhaseEndTime = System.currentTimeMillis();
    System.out.println(
        TestPhase.COOLDOWN.name()
            + " complete: Time "
            + (testPhaseEndTime - testPhaseStartTime) / 1000.0
            + " seconds");

    long endTime = System.currentTimeMillis();
    System.out.println("===========================================");

    TestPhaseStats overallStats;
    if (USE_SINGLE_THREAD_STAT_RECORDER) {
      List<List<StatsRecorderST.BucketStats>> threadsBucketStats = new ArrayList<>();
      for (StatsRecorderST st : statsRecorderSTList) {
        threadsBucketStats.add(st.getBucketStats());
      }
      List<StatsRecorderST.BucketStats> overallBucketStats = StatsRecorderST.aggregateStatsPerWindow(threadsBucketStats);
      overallStats = toTestPhaseStats(overallBucketStats);
    } else {
      overallStats = new TestPhaseStats();
      overallStats.numTotalRequests = statsRecorder.getNumTotalRequests();
      overallStats.numSuccessfullRequests = statsRecorder.getNumSuccessfullRequests();
      overallStats.requestsSentPerSecond = statsRecorder.getGeneratedRequestsPerTimeWindow();
      overallStats.requestsProcessedPerSecond = statsRecorder.getSuccessfullRequestsPerTimeWindow();
      overallStats.p99LatencyMs = statsRecorder.getPercentileLatency(0.99);
      overallStats.p95LatencyMs = statsRecorder.getPercentileLatency(0.95);
    }
    overallStats.totalTimeMs = endTime - startTime;
    overallStats.numTestIterations =
        testParam.numTests
            * (warmupPhaseProp.getPhaseLength()
            + loadingPhaseProp.getPhaseLength()
            + peakPhaseProp.getPhaseLength()
            + cooldownPhaseProp.getPhaseLength());

    System.out.println("Total number of requests sent: " + overallStats.numTotalRequests);
    System.out.println(
        "Total number of Successful responses: " + overallStats.numSuccessfullRequests);
    double runTimeSec = (endTime - startTime) / 1000.0;
    System.out.println("Test Wall Time: " + runTimeSec + " seconds");

    System.out.println(
        "Overall throughput across all phases: "
            + overallStats.numTotalRequests / runTimeSec
            + " rps.");

    System.out.println("P95 Latency = " + overallStats.p95LatencyMs + " ms.");
    System.out.println("P99 Latency = " + overallStats.p99LatencyMs + " ms.");

    // just wait and join all threads
    for (Thread t : threads) {
      t.join();
    }

    return overallStats;
  }
}
