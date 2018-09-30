/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author manika2211
 */
public class ClientTestThread extends Thread {
    public static class TestRunStats {
        ArrayList<Long> latencyValues = new ArrayList<Long>();
        long totalRequests = 0;
        long numSucceeded = 0;
    }
    
    ClientTestThread(ClientTest ct, int numIterations) {
        this.ct = ct;
        this.numIterations = numIterations;
        this.testRunStats = new TestRunStats();
        this.testRunStats.latencyValues.ensureCapacity(this.numIterations * 2);
        this.threadFinished = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        Stopwatch requestTimer = Stopwatch.createUnstarted();
        for (int j = 0; j < numIterations; j++) {
            // ================= Get Request =================== //
            requestTimer.reset();
            requestTimer.start();
            try {
                ct.getStatusCall(kFixedInputStr);
                requestTimer.stop();
                this.testRunStats.latencyValues.add(requestTimer.elapsed(TimeUnit.MICROSECONDS));
                this.testRunStats.numSucceeded++;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            this.testRunStats.totalRequests++;

            // ================= Post Request =================== //
            requestTimer.reset();
            requestTimer.start();
            try {
                ct.postDataCall(kFixedInputStr);
                this.testRunStats.numSucceeded++;
                requestTimer.stop();
                this.testRunStats.latencyValues.add(requestTimer.elapsed(TimeUnit.MICROSECONDS));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            this.testRunStats.totalRequests++;
        }
        this.threadFinished.set(true);
    }

    public TestRunStats getTestRunStats() {
        if (!this.threadFinished.get()) {
          throw new IllegalStateException(" thread not finished yet.");
        }
       return this.testRunStats;
    }
    
    public long getNumRequests() {
        if (!this.threadFinished.get()) {
          throw new IllegalStateException(" thread not finished yet.");
        }
        return this.testRunStats.totalRequests;
    }

    public long getNumSuccessfullRequests() {
        if (!this.threadFinished.get()) {
          throw new IllegalStateException(" thread not finished yet.");
        }
        return this.testRunStats.numSucceeded;
    }

    public List<Long> getLatencyValues() {
        if (!this.threadFinished.get()) {
          throw new IllegalStateException(" thread not finished yet.");
        }
       return this.testRunStats.latencyValues;
    }
    
    private final ClientTest ct;
    private final int numIterations;
    
    private AtomicBoolean threadFinished;
    
    private final TestRunStats testRunStats;
    private final String kFixedInputStr = "abcd";

}
