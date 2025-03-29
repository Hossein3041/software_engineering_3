package com.example.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.app.utils.PerformanceLogger;
import com.example.app.utils.SimpleDbLatency;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginScalabilityTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void runScalabilityTest(
            String baseUrl,
            int startUsers,
            int maxUsers,
            int stepSize) throws Exception {

        System.out.println("=== Login Scalability Test: Regular vs Virtual Thread Login ===");
        System.out.println("Base URL: " + baseUrl);
        System.out.println("User range: " + startUsers + " to " + maxUsers + " (step: " + stepSize + ")");
        System.out.println("Requests per user: 1");
        System.out.println("DB Latency: Disabled");
        System.out.println();

        String testName = "login_thread_comparison";

        System.out.println("\n========== TESTING REGULAR LOGIN ENDPOINT ==========");
        List<Map<String, Object>> regularMetricsList = new ArrayList<>();

        for (int userCount = startUsers; userCount <= maxUsers; userCount += stepSize) {
            System.out.println("\n=== Testing regular login with " + userCount + " concurrent users ===");

            PerformanceLogger regularLogger = new PerformanceLogger(testName, "regular", userCount);
            regularLogger.initialize();

            testEndpoint(
                    baseUrl + "/api/login",
                    userCount,
                    regularLogger);

            regularLogger.writeMetrics();
            Map<String, Object> metrics = regularLogger.getCurrentMetrics();
            regularMetricsList.add(metrics);

            System.out.println("Average response time: " + metrics.get("avgResponseTimeMs") + " ms");
            System.out.println(
                    "Requests per second: " + String.format("%.2f", (double) metrics.get("requestsPerSecond")));
            System.out.println("Success rate: " +
                    (((int) metrics.get("successCount") * 100.0) / (int) metrics.get("totalRequests")) + "%");

            regularLogger.close();

            System.out.println("Cooling down for 5 seconds...");
            Thread.sleep(5000);
        }

        System.out.println("\n========== TESTING VIRTUAL THREAD LOGIN ENDPOINT ==========");
        List<Map<String, Object>> vtMetricsList = new ArrayList<>();

        int countIndex = 0;
        for (int userCount = startUsers; userCount <= maxUsers; userCount += stepSize) {
            System.out.println("\n=== Testing virtual thread login with " + userCount + " concurrent users ===");

            PerformanceLogger vtLogger = new PerformanceLogger(testName, "virtual-thread", userCount);
            vtLogger.initialize();

            testEndpoint(
                    baseUrl + "/api/vt/login",
                    userCount,
                    vtLogger);

            vtLogger.writeMetrics();
            Map<String, Object> metrics = vtLogger.getCurrentMetrics();
            vtMetricsList.add(metrics);

            System.out.println("Average response time: " + metrics.get("avgResponseTimeMs") + " ms");
            System.out.println(
                    "Requests per second: " + String.format("%.2f", (double) metrics.get("requestsPerSecond")));
            System.out.println("Success rate: " +
                    (((int) metrics.get("successCount") * 100.0) / (int) metrics.get("totalRequests")) + "%");

            vtLogger.close();

            if (countIndex < regularMetricsList.size()) {
                Map<String, Object> regularMetrics = regularMetricsList.get(countIndex);

                double regularRps = (double) regularMetrics.get("requestsPerSecond");
                double vtRps = (double) metrics.get("requestsPerSecond");

                System.out.println("\n----- COMPARISON AT " + userCount + " USERS -----");
                System.out.println("Regular login avg response time: " +
                        regularMetrics.get("avgResponseTimeMs") + " ms");
                System.out.println("Virtual thread login avg response time: " +
                        metrics.get("avgResponseTimeMs") + " ms");
                System.out.println("Regular login requests/sec: " + String.format("%.2f", regularRps));
                System.out.println("Virtual thread requests/sec: " + String.format("%.2f", vtRps));

                if (regularRps > 0) {
                    double improvement = vtRps / regularRps;
                    System.out.println("Throughput improvement: " + String.format("%.2fx", improvement));
                }
            }

            countIndex++;

            System.out.println("Cooling down for 3 seconds...");
            Thread.sleep(3000);
        }

        System.out.println("\n========== FINAL COMPARISON SUMMARY ==========");
        System.out.println("User Count | Regular RT (ms) | VT RT (ms) | Regular RPS | VT RPS | Improvement");
        System.out.println("--------------------------------------------------------------------------");

        int index = 0;
        int count = startUsers;
        while (index < regularMetricsList.size() && index < vtMetricsList.size()) {
            Map<String, Object> regMetrics = regularMetricsList.get(index);
            Map<String, Object> vtMetrics = vtMetricsList.get(index);

            double regRt = (double) regMetrics.get("avgResponseTimeMs");
            double vtRt = (double) vtMetrics.get("avgResponseTimeMs");
            double regRps = (double) regMetrics.get("requestsPerSecond");
            double vtRps = (double) vtMetrics.get("requestsPerSecond");
            double improvement = regRps > 0 ? vtRps / regRps : 0;

            System.out.printf("%-10d | %-15.2f | %-10.2f | %-11.2f | %-6.2f | %-11.2fx%n",
                    count, regRt, vtRt, regRps, vtRps, improvement);

            index++;
            count += stepSize;
        }
    }

    private static void testEndpoint(
            String url,
            int userCount,
            PerformanceLogger logger) throws Exception {

        System.out.println(
                "Testing " + url + " with " + userCount + " concurrent users (all requests sent simultaneously)");

        final AtomicInteger completedRequests = new AtomicInteger(0);

        try (ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .executor(clientExecutor)
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();

            Map<String, String> loginData = Map.of(
                    "email", "momo@gmail.com",
                    "password", "123456");
            String loginJson = objectMapper.writeValueAsString(loginData);

            List<CompletableFuture<Void>> allFutures = new ArrayList<>();

            long testStartTime = System.currentTimeMillis();

            for (int user = 0; user < userCount; user++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        long requestStart = System.currentTimeMillis();

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                                .build();

                        HttpResponse<String> response = httpClient.send(
                                request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() >= 400) {
                            System.out.println("Request failed with status code: " + response.statusCode());
                        }

                        long requestEnd = System.currentTimeMillis();
                        long duration = requestEnd - requestStart;

                        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                        logger.logResult(success, duration);

                    } catch (Exception e) {
                        logger.logResult(false, 0); // Count as failure with no response time
                        System.out.println("Request error: " + e.getMessage());
                    } finally {
                        completedRequests.incrementAndGet();
                    }
                }, clientExecutor);

                allFutures.add(future);
            }

            CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                    allFutures.toArray(new CompletableFuture[0]));

            try {
                allRequests.get();
            } catch (Exception e) {
                System.out.println("Some requests failed to complete: " + e.getMessage());
            }

            long testEndTime = System.currentTimeMillis();
            double testDurationSeconds = (testEndTime - testStartTime) / 1000.0;

            System.out.println("Test completed in " + String.format("%.2f", testDurationSeconds) +
                    " seconds, " + completedRequests.get() + "/" + userCount +
                    " requests completed");

            logger.writeMetrics();
        }
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = "http://localhost:8080";
        int startUsers = 100;
        int maxUsers = 300;
        int stepSize = 20;
        runScalabilityTest(baseUrl, startUsers, maxUsers, stepSize);
    }
}