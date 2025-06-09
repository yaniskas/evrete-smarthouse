package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.stream.Collectors;

import com.example.MsgTypes.Action;

public class SmartHomeMonitorBenchmark {

    private int repetitions;

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
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

    private boolean withHeavyGuards;

    public boolean isWithHeavyGuards() {
        return withHeavyGuards;
    }

    public SmartHomeMonitorBenchmark(List<BenchmarkData> monitorData, int warmupRepetitions, int repetitions, boolean withHeavyGuards) {
        this.monitorData = monitorData;
        this.warmupRepetitions = warmupRepetitions;
        this.repetitions = repetitions;
        this.withHeavyGuards = withHeavyGuards;
    }

    public Measurement measureMonitor(BenchmarkData benchmarkData) throws InterruptedException, ExecutionException {
        Long startTime = null;
        Long endTime = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var monitor = new MonitorMatcher(
                    new LinkedTransferQueue<Action>(),
                    new SmartHouseMonitor(this.withHeavyGuards));

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
        System.out.println("Starting Warmup");

        try {
            for (int i = 0; i < warmupRepetitions ; i++) {
                System.out.println("Running warmup benchmark " + i);
                var benchmarkData = monitorData.get(i);

                if (repetitions > 1) {
                    for (int r = 0; r < repetitions; r++) {
                        System.out.println("\tRepetition " + r);
                        measureMonitor(benchmarkData);
                    }
                } else {
                    measureMonitor(benchmarkData);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Warmup Finished");
    }

    public void runBenchmark(String dataDirPath) {
        // Collect the measurements from the benchmark repetitions
        System.out.println("Starting Benchmark");
        Map<Integer, List<Long>> benchmarkResults = new TreeMap<>();

        try {
            int benchi = 0;
            for (BenchmarkData benchmarkData : monitorData) {
                System.out.println("Running benchmark " + benchi);
                benchi++;
                List<Measurement> measurements = new ArrayList<>();

                if (repetitions > 1) {
                    for (int r = 0; r < repetitions; r++) {
                        System.out.println("\tRepetition " + r);
                        measurements.add(measureMonitor(benchmarkData));
                    }
                } else {
                    measurements.add(measureMonitor(benchmarkData));
                }

                List<Long> times = measurements.stream().map(m -> m.time().toMillis()).collect(Collectors.toList());
                benchmarkResults.put(benchmarkData.numberOfRandomFacts(), times);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        String timestamp   = null;
        String dataDirName = null;

        if (withHeavyGuards) {
            timestamp   = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
            dataDirName = String.format("%s/%s_Evrete_SmartHouseWithHeavyGuards", dataDirPath, timestamp);
        } else {
            timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
            dataDirName = String.format("%s/%s_Evrete_SmartHouse", dataDirPath, timestamp);
        }

        try {
            Files.createDirectories(Paths.get(dataDirName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SmartHomeMonitorBenchmark.writeToCsv(benchmarkResults, dataDirName + "/Evrete_SmartHouse.csv");
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