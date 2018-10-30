package com.project.bsds.bsdsassignment;

import com.google.common.base.Stopwatch;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.project.bsds.bsdsassignment.StepCounterObjects.*;

public class StepCounterLoadGenerator {
  // test related options
  @Option(name = "-t", usage = "max threads at peak phase")
  private int maxThreads = 64;

  @Option(name = "-d", usage = "day number to generate date for")
  private int dayNum = 1;

  @Option(name = "-", usage = "user population size")
  private int userPopulationSize = 100000;

  @Option(name = "-n", usage = "number of tests")
  private int numTests = 100;

  // server related options
  @Option(name = "-h", usage = "ip address of server")
  private String serverIp =
      "ec2-34-220-61-97.us-west-2.compute.amazonaws.com"; // <---- ec2 end point
//      "bsdsassignment2-load-balancer-2101220617.us-west-2.elb.amazonaws.com";  // <--- load balancer end point
//      "localhost";

  @Option(name = "-p", usage = "port used on server")
  private int serverPort = 8080;

  @Option(name = "-u", usage = "uri for request")
  private String serverUri = "/bsdsassignment2-webapp/stepcounter";

  @Option(name = "-g", usage = "generated graph path")
  private String graphPath = "/tmp/StepCounterThroughput.jpeg";

  @Option(name = "-o", usage = "overlapping test phases")
  private boolean testPhasesOverlapping = true;

  @Option(name = "-r", usage = "stats recorder single threaded")
  private boolean useSingleThreadStatRecorder = true;


  public StepCounterLoadGenerator() {}

  public static void main(String[] args)
      throws IOException, InterruptedException, BrokenBarrierException {
    StepCounterLoadGenerator t = new StepCounterLoadGenerator();
    TestParam tp = t.init(args);
    if (t.testPhasesOverlapping) {
      t.runTestPhaseOverlapping(tp);
    } else {
      t.startNonOverlapping(tp);
    }
  }

  public TestParam init(String[] args) throws IOException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      parser.printUsage(System.err);
      throw new IOException(e);
    }

    StepCounterClient sc = new StepCounterClient(serverIp, serverPort, serverUri);
    sc.clearData(); // clear database before starting tests
    return new TestParam(
        serverIp, serverPort, serverUri, maxThreads, numTests, dayNum, userPopulationSize);
  }

  void plotResults(TestPhaseStats testStats) throws IOException {
    XYSeries requestGeneratedSeries = new XYSeries("generated");
    XYSeries throughputSeries = new XYSeries("throughput");

    Integer startTime = 1;
    for (int i = 0;
         i
             < Math.min(
             testStats.requestsProcessedPerSecond.length,
             testStats.requestsSentPerSecond.length);
         i++) {
//      if (testStats.requestsSentPerSecond[i] < 500 /* ignore low values */) {
//        continue;
//      }
      requestGeneratedSeries.add((int) startTime, (int) testStats.requestsSentPerSecond[i]);
      throughputSeries.add((int) startTime, (int) testStats.requestsProcessedPerSecond[i]);
      startTime++;
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(requestGeneratedSeries);
    dataset.addSeries(throughputSeries);

    NumberAxis domain = new NumberAxis("Time");
    NumberAxis range = new NumberAxis("Requests Per second");

    XYSplineRenderer renderer = new XYSplineRenderer(50);
    renderer.setBaseShapesVisible(false);
    XYPlot xyplot = new XYPlot(dataset, domain, range, renderer);
    JFreeChart chart =
        new JFreeChart(
            "Overall Throughput "
                + maxThreads
                + " threads "
                + numTests// testStats.numTestIterations
                + " iterations",
            xyplot);

    int width = 640; /* Width of the image */
    int height = 480; /* Height of the image */
    File lineChart = new File(graphPath);
    ChartUtilities.saveChartAsJPEG(lineChart, chart, width, height);
  }

  void runTestPhaseOverlapping(TestParam testParam) throws IOException, InterruptedException, BrokenBarrierException {
    StepCounterThreadManager tm = new StepCounterThreadManager(testParam);
    TestPhaseStats testPhaseStats = tm.start();
    plotResults(testPhaseStats);
  }

  TestPhaseStats runTestPhaseNonOverlapping(
      TestPhase testPhase,
      TestParam testParam,
      AtomicInteger threadIdCounter,
      long relativeStartTimeMs)
      throws InterruptedException {
    TestPhaseProp testPhaseProp = getTestPhaseProp(testPhase);
    if (testPhaseProp == null) {
      throw new IllegalStateException("invalid test phase.");
    }
    final String phaseName = testPhase.name() + " phase";

    int numThreads = testPhaseProp.getPhaseNumThreads(maxThreads);
    StatsRecorder statsRecorder = useSingleThreadStatRecorder ? null : new StatsRecorder();
    StatsRecorderST[] statsRecorderSTList = new StatsRecorderST[numThreads];
    if (useSingleThreadStatRecorder) {
      for (int i = 0; i < numThreads; i++) {
        statsRecorderSTList[i] = new StatsRecorderST(relativeStartTimeMs);
      }
    }
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      threads.add(
          new StepCounterClientTestThread(
              threadIdCounter.getAndIncrement(),
              testParam,
              testPhase,
              statsRecorder,
              statsRecorderSTList[i]));
    }
    long startTime = System.currentTimeMillis();
    for (Thread t : threads) {
      t.start();
    }
    System.out.println(phaseName + ": All threads(" + numThreads + ") running....");
    for (Thread t : threads) {
      t.join();
    }
    long endTime = System.currentTimeMillis();
    System.out.println(
        phaseName + " complete: Time " + (endTime - startTime) / 1000.0 + " seconds");
    TestPhaseStats testPhaseStats;
    if (useSingleThreadStatRecorder) {
      List<List<StatsRecorderST.BucketStats>> threadsBucketStats = new ArrayList<>();
      for (StatsRecorderST st : statsRecorderSTList) {
        threadsBucketStats.add(st.getBucketStats());
      }
      List<StatsRecorderST.BucketStats> overallBucketStats =
          StatsRecorderST.aggregateStatsPerWindow(threadsBucketStats);
      testPhaseStats = toTestPhaseStats(overallBucketStats);
    } else {
      testPhaseStats = new TestPhaseStats();
      testPhaseStats.numTotalRequests = statsRecorder.getNumTotalRequests();
      testPhaseStats.numSuccessfullRequests = statsRecorder.getNumSuccessfullRequests();
      testPhaseStats.requestsSentPerSecond = statsRecorder.getGeneratedRequestsPerTimeWindow();
      testPhaseStats.requestsProcessedPerSecond =
          statsRecorder.getSuccessfullRequestsPerTimeWindow();
      testPhaseStats.p99LatencyMs = statsRecorder.getPercentileLatency(0.99);
      testPhaseStats.p95LatencyMs = statsRecorder.getPercentileLatency(0.95);
    }
    testPhaseStats.totalTimeMs = endTime - startTime;
    testPhaseStats.numTestIterations = numTests * testPhaseProp.getPhaseLength();
    return testPhaseStats;
  }


  void startNonOverlapping(TestParam testParam) throws InterruptedException, IOException {
    long startTime = System.currentTimeMillis();
    System.out.println("Client starting.... Time: " + startTime);
    AtomicInteger threadIdCounter = new AtomicInteger(0);
    Stopwatch relativeTimer = Stopwatch.createStarted();
    TestPhaseStats ts1 = runTestPhaseNonOverlapping(TestPhase.WARMUP, testParam, threadIdCounter, 0);
    TestPhaseStats ts2 =
        runTestPhaseNonOverlapping(
            TestPhase.LOADING,
            testParam,
            threadIdCounter,
            relativeTimer.elapsed(TimeUnit.MILLISECONDS));
    TestPhaseStats ts3 =
        runTestPhaseNonOverlapping(
            TestPhase.PEAK,
            testParam,
            threadIdCounter,
            relativeTimer.elapsed(TimeUnit.MILLISECONDS));
    TestPhaseStats ts4 =
        runTestPhaseNonOverlapping(
            TestPhase.COOLDOWN,
            testParam,
            threadIdCounter,
            relativeTimer.elapsed(TimeUnit.MILLISECONDS));
    long endTime = System.currentTimeMillis();
    System.out.println("===========================================");
    TestPhaseStats overallStats = aggregateStats(ts1, ts2, ts3, ts4);
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

    plotResults(overallStats);
  }
}
