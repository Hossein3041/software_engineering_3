package com.example.app.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceLogger {
    private static final String LOG_DIRECTORY = "performance_logs";

    private String testName;
    private String filePath;
    private BufferedWriter writer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final ConcurrentHashMap<Integer, AtomicInteger> responseTimes = new ConcurrentHashMap<>();

    private int concurrentUsers;
    private String testType;
    private long testStartTime;

    public PerformanceLogger(String testName, String testType, int concurrentUsers) {
        this.testName = testName;
        this.testType = testType;
        this.concurrentUsers = concurrentUsers;
        this.testStartTime = System.currentTimeMillis();
    }

    public void initialize() {
        if (initialized.getAndSet(true)) {
            return;
        }

        try {
            Path directory = Paths.get(LOG_DIRECTORY);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            filePath = LOG_DIRECTORY + "/" + testName + "_" + testType + "_" + timestamp + ".csv";

            writer = new BufferedWriter(new FileWriter(filePath));

            writer.write("timestamp,elapsed_seconds,concurrent_users,test_type,requests,successes,failures," +
                    "avg_response_time,min_response_time,max_response_time,requests_per_second\n");

            System.out.println("Performance logging started: " + filePath);
        } catch (IOException e) {
            System.err.println("Error initializing performance logger: " + e.getMessage());
            initialized.set(false);
        }
    }

    public void logResult(boolean success, long responseTimeMs) {
        requestCount.incrementAndGet();

        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }

        totalResponseTime.addAndGet(responseTimeMs);
        updateMinResponseTime(responseTimeMs);
        updateMaxResponseTime(responseTimeMs);

        int bucket = (int) (responseTimeMs / 100) * 100;
        responseTimes.computeIfAbsent(bucket, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void writeMetrics() {
        if (!initialized.get()) {
            initialize();
        }

        try {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - testStartTime) / 1000.0;

            int totalRequests = requestCount.get();
            int successes = successCount.get();
            int failures = failureCount.get();

            double avgResponseTime = totalRequests > 0 ? totalResponseTime.get() / (double) totalRequests : 0;

            long minRespTime = minResponseTime.get();
            if (minRespTime == Long.MAX_VALUE) {
                minRespTime = 0;
            }

            long maxRespTime = maxResponseTime.get();

            double requestsPerSecond = elapsedSeconds > 0 ? totalRequests / elapsedSeconds : 0;

            writer.write(String.format("%d,%.2f,%d,%s,%d,%d,%d,%.2f,%d,%d,%.2f\n",
                    now, elapsedSeconds, concurrentUsers, testType,
                    totalRequests, successes, failures,
                    avgResponseTime, minRespTime, maxRespTime, requestsPerSecond));
            writer.flush();

        } catch (IOException e) {
            System.err.println("Error writing metrics: " + e.getMessage());
        }
    }

    public void writeDistribution() {
        if (!initialized.get()) {
            return;
        }

        String distributionFilePath = filePath.replace(".csv", "_distribution.csv");

        try (BufferedWriter distWriter = new BufferedWriter(new FileWriter(distributionFilePath))) {
            distWriter.write("response_time_bucket,count\n");

            responseTimes.forEach((bucket, count) -> {
                try {
                    distWriter.write(bucket + "," + count.get() + "\n");
                } catch (IOException e) {
                    System.err.println("Error writing distribution data: " + e.getMessage());
                }
            });

            System.out.println("Response time distribution written to: " + distributionFilePath);
        } catch (IOException e) {
            System.err.println("Error creating distribution file: " + e.getMessage());
        }
    }

    public void close() {
        if (!initialized.get()) {
            return;
        }

        try {
            writeMetrics();
            writeDistribution();
            writer.close();
            System.out.println("Performance log finalized: " + filePath);
        } catch (IOException e) {
            System.err.println("Error closing performance logger: " + e.getMessage());
        }
    }

    public Map<String, Object> getCurrentMetrics() {
        int totalRequests = requestCount.get();
        double avgResponseTime = totalRequests > 0 ? totalResponseTime.get() / (double) totalRequests : 0;

        long minRespTime = minResponseTime.get();
        if (minRespTime == Long.MAX_VALUE) {
            minRespTime = 0;
        }

        long elapsedMs = System.currentTimeMillis() - testStartTime;
        double requestsPerSecond = elapsedMs > 0 ? (totalRequests * 1000.0) / elapsedMs : 0;

        return Map.of(
                "testType", testType,
                "concurrentUsers", concurrentUsers,
                "totalRequests", totalRequests,
                "successCount", successCount.get(),
                "failureCount", failureCount.get(),
                "avgResponseTimeMs", avgResponseTime,
                "minResponseTimeMs", minRespTime,
                "maxResponseTimeMs", maxResponseTime.get(),
                "requestsPerSecond", requestsPerSecond);
    }

    private void updateMinResponseTime(long responseTime) {
        long currentMin = minResponseTime.get();
        while (responseTime < currentMin) {
            if (minResponseTime.compareAndSet(currentMin, responseTime)) {
                break;
            }
            currentMin = minResponseTime.get();
        }
    }

    private void updateMaxResponseTime(long responseTime) {
        long currentMax = maxResponseTime.get();
        while (responseTime > currentMax) {
            if (maxResponseTime.compareAndSet(currentMax, responseTime)) {
                break;
            }
            currentMax = maxResponseTime.get();
        }
    }
}
