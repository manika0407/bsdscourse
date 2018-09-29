/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Stopwatch;
import static com.google.common.math.Quantiles.median;
import static com.google.common.math.Quantiles.percentiles;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author manika2211
 */
public class ThreadsLoadClient {
    
    class TestThread extends Thread {
        
        TestThread(ClientTest ct, int numIterations) {
            this.numIterations = numIterations;
            this.latencyValues = new ArrayList<Long>();
        }
        
        @Override
        public void run() {
            this.latencyValues.clear();
            this.numFailed = 0;
            this.numSucceeded = 0;
            Stopwatch requestTimer = Stopwatch.createUnstarted();
            for (int j = 0; j < numIterations; j++) {
                requestTimer.reset();
                requestTimer.start();
                // TODO confirm if these are treated separate request
                try {
                    ct.getCall("abc");
                    this.numSucceeded++;
                } catch(RuntimeException e) {
                   this.numFailed++;
                }
                requestTimer.stop();
                this.latencyValues.add(requestTimer.elapsed(TimeUnit.MICROSECONDS));
                
                requestTimer.reset();
                requestTimer.start();
                try {
                    ct.postCall("abc");
                    this.numSucceeded++;
                } catch(RuntimeException e) {
                   this.numFailed++;
                }
                requestTimer.stop();
                this.latencyValues.add(requestTimer.elapsed(TimeUnit.MICROSECONDS));
                
            }
        }

        public long getNumRequests() {
            return this.numFailed + this.numSucceeded;
        }
        
        public long getNumSuccessfullRequests() {
            return this.numSucceeded;
        }
        
        public List<Long> getLatencyValues() {
            return this.latencyValues;
        }
        
        private ClientTest ct;
        private int numIterations;
        
        private List<Long> latencyValues; 
        private long numFailed, numSucceeded;
    
    }
    
    void testClient(String hostIp, int port, int numThreads, int numIterations) throws InterruptedException {
       ClientTest ct = new ClientTest(hostIp, port);
       List<Thread> threads = new ArrayList<Thread>();
       for (int i = 0; i < numThreads; i++) {
          threads.add(new TestThread(ct, numIterations));
       }
       System.out.println("Number of Threads = "+numThreads);
       System.out.println("Number of Iterations = "+numIterations);
       long startTime = System.nanoTime();
       for (Thread t : threads) {
           t.start();
       }
        System.out.println("Threads started.");
       for (Thread t : threads) {
           t.join();
       }
       System.out.println("Threads finished.");
       long endTime = System.nanoTime();
       System.out.println("time taken = " + (endTime - startTime) / 1000 + " us.");
       
       long totalRequests = 0, successfullRequests = 0;
       List<Long> overallLatencies = new ArrayList<Long>();
       for (Thread t : threads) {
           TestThread tt = ((TestThread)t);
           totalRequests += tt.getNumRequests();
           successfullRequests += tt.getNumSuccessfullRequests();
           overallLatencies.addAll(tt.getLatencyValues());
       }
       
       long sumLatencies = 0;
       for (long l : overallLatencies) {
        sumLatencies += l;
       }
       double meanLatency = sumLatencies / (1.0 * overallLatencies.size());
       
       Collections.sort(overallLatencies);
       Map<Integer, Double> pctLatencies = percentiles().indexes(50, 95, 99).compute(overallLatencies);
       double medianLatency = pctLatencies.get(50);
       double p95Latency = pctLatencies.get(95);
       double p99Latency = pctLatencies.get(99);
       System.out.println("Mean Latency for all requests = "+meanLatency);
       System.out.println("Median Latency = " + medianLatency + " us.");
       System.out.println("P95 Latency = " + p95Latency + " us.");
       System.out.println("P99 Latency = " + p99Latency + " us.");
       
    }
    
    public static void main(String[] args) throws InterruptedException{
        //TODO get hostIp, port, numThread, numIterations
        ThreadsLoadClient t = new ThreadsLoadClient();
        t.testClient("localhost", 8080, 100, 100);
    }

    
}
