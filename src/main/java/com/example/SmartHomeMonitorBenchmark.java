package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.stream.Collectors;

import com.example.MsgTypes.Action;

public class SmartHomeMonitorBenchmark {

    private int iterations;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    private int warmupRepetitions;

    public int getWarmupRepetitions() {
        return warmupRepetitions;
    }

    public void setWarmupRepetitions(int warmupRepetitions) {
        this.warmupRepetitions = warmupRepetitions;
    }

    private List<BenchmarkData> monitorData;

    public List<BenchmarkData> getMonitorData() {
        return monitorData;
    }

    public void setMonitorData(List<BenchmarkData> monitorData) {
        this.monitorData = monitorData;
    }

    public SmartHomeMonitorBenchmark(List<BenchmarkData> monitorData, int warmupRepetitions, int repetitions) {
        this.monitorData = monitorData;
        this.warmupRepetitions = warmupRepetitions;
        this.iterations = repetitions;
    }

    public Measurement measureMonitor(BenchmarkData benchmarkData) throws InterruptedException, ExecutionException {
        Long startTime = null;
        Long endTime = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var monitor = new MonitorMatcher(
                    new LinkedTransferQueue<Action>(),
                    new SmartHouseMonitor());

            var monitorMatcher = executor.submit(() -> {
                monitor.match();
            });

            // start
            startTime = System.currentTimeMillis();
            var sender = executor.submit(() -> {
                for (int i = 0; i < benchmarkData.numberOfFacts(); i++) {
                    monitor.send(benchmarkData.facts().get(i));
                }
            });

            sender.get();
            monitor.send(new Action.ShutOff());
            monitorMatcher.get();
            endTime = System.currentTimeMillis();
            // end
        }

        return new Measurement(Duration.ofMillis(endTime - startTime));
    }

    public void runWarmup() {
        // Collect the measurements from the warmup repetitions
        System.out.println("Starting Warmup");
        Map<Integer, Long> warmupResults = new TreeMap<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (BenchmarkData benchmarkData : monitorData) {
                List<Future<Measurement>> warmupFuts = new ArrayList<>();
                for (int i = 0; i < warmupRepetitions; i++) {
                    Future<Measurement> fut = executor.submit(() -> measureMonitor(benchmarkData));
                    warmupFuts.add(fut);
                }

                // Get the measurements from the futures
                List<Measurement> measurements = warmupFuts.stream().map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());

                Long averageTime = measurements.stream().mapToLong(m -> m.time().toMillis()).sum() / warmupRepetitions;
                warmupResults.put(benchmarkData.numberOfRandomFacts(), averageTime);
            }
        }
        System.out.println("Warmup Finished");
    }

    public void runBenchmark() {
        // Collect the measurements from the benchmark repetitions
        System.out.println("Starting Benchmark");
        Map<Integer, List<Long>> benchmarkResults = new TreeMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (BenchmarkData benchmarkData : monitorData) {
                List<Future<Measurement>> futures = new ArrayList<>();
                for (int i = 0; i < iterations; i++) {
                    Future<Measurement> fut = executor.submit(() -> measureMonitor(benchmarkData));
                    futures.add(fut);
                }

                List<Measurement> measurements = futures.stream().map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());

                List<Long> times = measurements.stream().map(m -> m.time().toMillis()).collect(Collectors.toList());
                benchmarkResults.put(benchmarkData.numberOfRandomFacts(), times);
            }
        }

        String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());

        SmartHomeMonitorBenchmark.writeToCsv(benchmarkResults,
                String.format("%s/data/%s_RETE_SmartHouse.csv",
                        System.getProperty("user.dir"), timestamp));
        System.out.println("Benchmark Finished");
    }

    public static void writeToCsv(Map<Integer, List<Long>> benchmarkResults, String filename) {
        // Write the benchmark results to a CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // Write the data
            for (Map.Entry<Integer, List<Long>> entry : benchmarkResults.entrySet()) {
                int numberOfRandomFacts = entry.getKey();
                List<Long> times = entry.getValue();
                String timesString = times.stream().map(Object::toString).collect(Collectors.joining(";"));
                String e = String.format("%d;%s\n", numberOfRandomFacts, timesString);
                writer.write(e);
            }
            System.out.println("Benchmark results written to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}