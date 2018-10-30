package com.project.bsds.bsdsassignment;

import com.netflix.hystrix.strategy.properties.HystrixProperty;
import com.netflix.hystrix.util.HystrixRollingNumber;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import com.netflix.hystrix.util.HystrixRollingPercentile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.google.common.math.Quantiles.percentiles;

// single thread stats recorder
class StatsRecorderST {
  public static class BucketStats {
    BucketStats(long startTimeMs) {
      this.startTimeMs = startTimeMs;
    }
    long startTimeMs;
    int totalRequests;
    int successfullRequests;
    int p99LatencyMs;
    int p95LatencyMs;
    int p50LatencyMs;

    @Override
    public String toString() {
      return "BucketStats{" +
          "startTimeMs=" + startTimeMs +
          ", totalRequests=" + totalRequests +
          ", successfullRequests=" + successfullRequests +
          ", p99LatencyMs=" + p99LatencyMs +
          ", p95LatencyMs=" + p95LatencyMs +
          ", p50LatencyMs=" + p50LatencyMs +
          '}';
    }
  }

  StatsRecorderST(long startTimeMs) {
    this.startTimeMs = (startTimeMs / BUCKET_WIDTH) * BUCKET_WIDTH;
    this.bucketId = 0; // == size of bucketStats
    this.bucketStats = new ArrayList<>(2000);
    this.currentStats = new BucketStats(this.startTimeMs);
    this.currentLatencies = new ArrayList<>(1000);
    this.stopped = false;
  }

  StatsRecorderST() {
    this(System.currentTimeMillis());
  }

  public void addValue(long requestTimeMs, int timeTakenMs, boolean succeeded) {
    if (this.stopped) {
      throw new IllegalStateException("not expected");
    }
    long nextStartTimeMs = ((bucketId + 1) * BUCKET_WIDTH) + startTimeMs;
    if (requestTimeMs >= nextStartTimeMs) {
      moveToNextBucket();
    }
    currentStats.totalRequests++;
    if (succeeded) {
      currentStats.successfullRequests++;
      currentLatencies.add(timeTakenMs);
    }
  }
  private void moveToNextBucket() {
    long nextStartTimeMs = ((bucketId + 1) * BUCKET_WIDTH) + startTimeMs;
    if (!currentLatencies.isEmpty()) {
      // record pct latencies from captured latencies for previous bucket
      Map<Integer, Double> pctLatencies =
          percentiles().indexes(50, 95, 99).compute(currentLatencies);
      currentStats.p50LatencyMs = pctLatencies.get(50).intValue();
      currentStats.p95LatencyMs = pctLatencies.get(95).intValue();
      currentStats.p99LatencyMs = pctLatencies.get(99).intValue();
    }
    // save previous bucket stats
    bucketStats.add(currentStats);
    currentStats = new BucketStats(nextStartTimeMs);
    currentLatencies.clear();
    // move to next bucket
    bucketId++;
  }

  void stop() {
    if (this.stopped) {
      return;
    }
    moveToNextBucket();
    this.stopped = true;
  }

  public List<BucketStats> getBucketStats() {
    stop();
    return bucketStats;
  }

  public static List<BucketStats> aggregateStatsPerWindow(List<List<BucketStats>> bss) {
    List<BucketStats> aggregateStats = new ArrayList<>();
    // get lowest start time from all buckets
    long startTimeMs = Long.MAX_VALUE;
    long endTimeMs = Long.MIN_VALUE;
    for (List<BucketStats> bs : bss) {
      if (!bs.isEmpty()) {
        startTimeMs = Math.min(startTimeMs, bs.get(0).startTimeMs);
        endTimeMs = Math.max(endTimeMs, bs.get(bs.size() - 1).startTimeMs);
      }
    }

    long currentTimeMs = startTimeMs;

    BucketStats currentBucketStats = new BucketStats(currentTimeMs);

    int[] toProcessIndex = new int[bss.size()];
    while (currentTimeMs <= endTimeMs) {
      for (int i = 0; i < bss.size(); i++) {
        List<BucketStats> bs = bss.get(i);
        if (toProcessIndex[i] >= bs.size()) {
          continue;
        }
        BucketStats bsi = bs.get(toProcessIndex[i]);
        if (bsi.startTimeMs > currentTimeMs) {
          continue;
        } else if (bsi.startTimeMs < currentTimeMs) {
          throw new IllegalStateException("shouldn't be here.");
        } else {
          currentBucketStats.totalRequests += bsi.totalRequests;
          currentBucketStats.successfullRequests += bsi.successfullRequests;
          if (currentBucketStats.p50LatencyMs == 0) {
            currentBucketStats.p50LatencyMs = bsi.p50LatencyMs;
          } else {
            currentBucketStats.p50LatencyMs = (bsi.p50LatencyMs + currentBucketStats.p50LatencyMs) / 2;
          }

          if (currentBucketStats.p95LatencyMs == 0) {
            currentBucketStats.p95LatencyMs = bsi.p95LatencyMs;
          } else {
            currentBucketStats.p95LatencyMs = (bsi.p95LatencyMs + currentBucketStats.p95LatencyMs) / 2;
          }

          if (currentBucketStats.p99LatencyMs == 0) {
            currentBucketStats.p99LatencyMs = bsi.p99LatencyMs;
          } else {
            currentBucketStats.p99LatencyMs = (bsi.p99LatencyMs + currentBucketStats.p99LatencyMs) / 2;
          }
          toProcessIndex[i]++;
        }
      }
      aggregateStats.add(currentBucketStats);
      currentTimeMs += BUCKET_WIDTH;
      currentBucketStats = new BucketStats(currentTimeMs);
    }
    for (int i = 0; i < bss.size(); i++) {
      if (toProcessIndex[i] != bss.get(i).size()) {
        throw new IllegalStateException("not expected");
      }
    }
    return aggregateStats;
  }

  int bucketId;
  long startTimeMs;
  private BucketStats currentStats;
  private List<Integer> currentLatencies;
  private List<BucketStats> bucketStats;
  private boolean stopped;
  private static final int BUCKET_WIDTH = 1000; // 1s

  public static void main(String[] args) {
    StatsRecorderST st1 = new StatsRecorderST(1000);
    StatsRecorderST st2 = new StatsRecorderST(1012);
    StatsRecorderST st3 = new StatsRecorderST(2100);

    st1.addValue(1200, 50, true);
    st1.addValue(1800, 75, true);
    st1.addValue(2200, 80, true);
    st1.addValue(2500, 50, true);
    st1.addValue(2800, 100, true);

    st2.addValue(1200, 50, true);
    st2.addValue(2200, 75, true);
    st2.addValue(2500, 80, true);
    st2.addValue(3200, 50, true);
    st2.addValue(4000, 100, true);

    st3.addValue(2200, 50, true);
    st3.addValue(2312, 75, true);
    st3.addValue(3210, 80, true);
    st3.addValue(4234, 50, true);
    st3.addValue(5240, 100, true);

    List<BucketStats> stb1 = st1.getBucketStats();
    System.out.println(stb1.toString());

    List<BucketStats> stb2 = st2.getBucketStats();
    System.out.println(stb2.toString());

    List<BucketStats> stb3 = st3.getBucketStats();
    System.out.println(stb3.toString());

    List<BucketStats> at = aggregateStatsPerWindow(Arrays.asList(stb1, stb2, stb3));
    System.out.println(at.toString());
  }
}


class StatsRecorder {

  public StatsRecorder() {
    requestsPerTimeWindow = new HystrixRollingNumber(MAX_RECORD_TIME, NUM_BUCKETS);
    latenciesPerTimeWindow =
        new HystrixRollingPercentile(
            MAX_RECORD_TIME,
            NUM_BUCKETS,
            BUCKET_WIDTH,
            HystrixProperty.Factory.asProperty(true));

    totalRequests = new AtomicInteger(0);
    successfullRequests = new AtomicInteger(0);

  }

  public void addValue(int timeMs, boolean succeeded) {
    requestsPerTimeWindow.increment(HystrixRollingNumberEvent.EMIT);
    totalRequests.incrementAndGet();
    if (succeeded) {
      requestsPerTimeWindow.increment(HystrixRollingNumberEvent.SUCCESS);
      latenciesPerTimeWindow.addValue(timeMs);
      successfullRequests.incrementAndGet();
    }
  }

  public long[] getSuccessfullRequestsPerTimeWindow() {
    return requestsPerTimeWindow.getValues(HystrixRollingNumberEvent.SUCCESS);
  }

  public long[] getGeneratedRequestsPerTimeWindow() {
    return requestsPerTimeWindow.getValues(HystrixRollingNumberEvent.EMIT);
  }

  public int getPercentileLatency(double percentile) {
    return latenciesPerTimeWindow.getPercentile(percentile);
  }

  public int getMeanLatency() {
    return latenciesPerTimeWindow.getMean();
  }

  public int getNumTotalRequests() {
    return totalRequests.get();
  }

  public int getNumSuccessfullRequests() {
    return successfullRequests.get();
  }

  private static final int MAX_RECORD_TIME = 30 * 60 * 1000; // 30 min
  private static final int BUCKET_WIDTH = 1000; // 1s
  private static final int NUM_BUCKETS = MAX_RECORD_TIME / BUCKET_WIDTH;

  private HystrixRollingNumber requestsPerTimeWindow;
  private HystrixRollingPercentile latenciesPerTimeWindow;

  private AtomicInteger totalRequests;
  private AtomicInteger successfullRequests;
}

