/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Stopwatch;
import static com.google.common.math.Quantiles.percentiles;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * @author manika2211
 */
public class ThreadsLoadClient {

    @Option(name = "-t", usage = "max threads")
    private int maxThreads = 50;
    @Option(name = "-n", usage = "number of iterations per thread")
    private int numIterations = 100;
    @Option(name = "-h", usage = "ip address of server")
    private String serverIp = "localhost";
    @Option(name = "-p", usage = "port used on server")
    private int serverPort = 8080;
    @Option(name = "-u", usage = "uri for request")
    private String serverUri = "/bsdsassignment-webapp/rest/hello/rs";
    
    enum TestPhase {
      WARMUP,
      LOADING,
      PEAK,
      COOLDOWN
    }
    private Map<TestPhase, Double> testPhaseThreadPctMap;
    
    public ThreadsLoadClient() {
        testPhaseThreadPctMap = new HashMap<TestPhase, Double>();
        testPhaseThreadPctMap.put(TestPhase.WARMUP, 0.1);
        testPhaseThreadPctMap.put(TestPhase.LOADING, 0.5);
        testPhaseThreadPctMap.put(TestPhase.PEAK, 1.0);
        testPhaseThreadPctMap.put(TestPhase.COOLDOWN, 0.25);
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        ThreadsLoadClient t = new ThreadsLoadClient();
        t.parseArgs(args);
        t.start();
    }
    
    public void parseArgs(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.err);
            throw new IOException(e);
        }
    }
    
    List<ClientTestThread.TestRunStats> runTestPhase(TestPhase testPhase) throws InterruptedException {
        Double testPhaseThreadPct = testPhaseThreadPctMap.get(testPhase);
        if (testPhaseThreadPct == null) {
            throw new IllegalStateException("invalid test phase.");
        }
        final String phaseName = testPhase.name() + " phase";
        ClientTest ct = new ClientTest(serverIp, serverPort, serverUri);
        int numThreads = (int) (maxThreads * testPhaseThreadPct);
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < numThreads; i++) {
            threads.add(new ClientTestThread(ct, numIterations));
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
        System.out.println(phaseName + " complete: Time " + (endTime - startTime) / 1000.0 + " seconds");
        
        List<ClientTestThread.TestRunStats> threadRunStats = new ArrayList<ClientTestThread.TestRunStats>();
        for (Thread t : threads) {
            ClientTestThread tt = ((ClientTestThread) t);
            threadRunStats.add(tt.getTestRunStats());
        }
        return threadRunStats;
    }
    
    ClientTestThread.TestRunStats aggregateStats(List<ClientTestThread.TestRunStats>... tsArr) {
        ClientTestThread.TestRunStats result = new ClientTestThread.TestRunStats();
        for (List<ClientTestThread.TestRunStats> ts : tsArr) {
            for (ClientTestThread.TestRunStats t : ts) {
               result.totalRequests += t.totalRequests;
               result.numSucceeded += t.numSucceeded;
               result.latencyValues.addAll(t.latencyValues);
            }
        }
        return result;
    }
    
    void start() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("Client starting.... Time: " + startTime);
        List<ClientTestThread.TestRunStats> ts1 = runTestPhase(TestPhase.WARMUP);
        List<ClientTestThread.TestRunStats> ts2 = runTestPhase(TestPhase.LOADING);
        List<ClientTestThread.TestRunStats> ts3 = runTestPhase(TestPhase.PEAK);
        List<ClientTestThread.TestRunStats> ts4 = runTestPhase(TestPhase.COOLDOWN);
        long endTime = System.currentTimeMillis();
        System.out.println("===========================================");
        ClientTestThread.TestRunStats overallStats = aggregateStats(ts1, ts2, ts3, ts4);
        System.out.println("Total number of requests sent: " + overallStats.totalRequests);
        System.out.println("Total number of Successful responses: " + overallStats.numSucceeded);
        double runTimeSec = (endTime - startTime) / 1000.0;
        System.out.println("Test Wall Time: " + runTimeSec + " seconds");
        
        System.out.println("Overall throughput across all phases: " + overallStats.totalRequests / runTimeSec + " rps.");
        
        long sumLatencies = 0;
        for (long l : overallStats.latencyValues) {
            sumLatencies += l;
        }
        
        double meanLatency = sumLatencies / (1.0 * overallStats.latencyValues.size());

        Collections.sort(overallStats.latencyValues);
        Map<Integer, Double> pctLatencies = percentiles().indexes(50, 95, 99).compute(overallStats.latencyValues);
        double medianLatency = pctLatencies.get(50);
        double p95Latency = pctLatencies.get(95);
        double p99Latency = pctLatencies.get(99);

        System.out.println("Mean Latency for all requests = " + meanLatency / 1000.0 + " ms.");
        System.out.println("Median Latency = " + medianLatency / 1000.0 + " ms.");
        System.out.println("P95 Latency = " + p95Latency / 1000.0 + " ms.");
        System.out.println("P99 Latency = " + p99Latency / 1000.0 + " ms.");
    }
}
