package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.Action;

public class SmartHomeMonitorBenchmark {

    static ExecutorService ec = Executors.newVirtualThreadPerTaskExecutor();
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

    public SmartHomeMonitorBenchmark(List<BenchmarkData> monitorData, int warmupRepetitions, int iterations) {
        this.monitorData = monitorData;
        this.warmupRepetitions = warmupRepetitions;
        this.iterations = iterations;
    }

    public Measurement measureMonitor(BenchmarkData benchmarkData) {
        SmartHouseMonitor monitor = new SmartHouseMonitor();
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = monitor.createMonitorKnowledge(service);
        List<Action> facts = benchmarkData.facts();
        try (StatefulSession session = knowledge.newStatefulSession()) {
            Measurement m = monitor.processFacts(knowledge, session, facts);
            return m;
        }
    }

    public void runWarmup() {
        // Collect the measurements from the warmup repetitions
        Map<Integer, Long> warmupResults = new TreeMap<>();

        for (BenchmarkData benchmarkData : monitorData) {
            List<CompletableFuture<Measurement>> futures = IntStream.range(0, warmupRepetitions)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> measureMonitor(benchmarkData), ec))
                    .collect(Collectors.toList());

            // Get the measurements from the futures
            List<Measurement> measurements = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            Long averageTime = measurements.stream().mapToLong(m -> m.time().toMillis()).sum() / warmupRepetitions;
            warmupResults.put(benchmarkData.numberOfRandomFacts(), averageTime);

            System.out.println("MEASUREMENTS: " + measurements.size());
            System.out.println(String.format("Number of facts: %d, Number of random facts: %d, Elapsed time: %s (ms)",
                    benchmarkData.numberOfFacts(), benchmarkData.numberOfRandomFacts(), averageTime));
        }

        System.out.println("Warmup measurements: " + warmupResults);
    }

    public void runBenchmark() {
        // Collect the measurements from the benchmark repetitions
        Map<Integer, List<Long>> benchmarkResults = new TreeMap<>();

        for (BenchmarkData benchmarkData : monitorData) {
            List<CompletableFuture<Measurement>> futures = IntStream.range(0, iterations)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> measureMonitor(benchmarkData), ec))
                    .collect(Collectors.toList());

            // Get the measurements from the futures
            List<Measurement> measurements = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            List<Long> times = measurements.stream().map(m -> m.time().toMillis()).collect(Collectors.toList());
            benchmarkResults.put(benchmarkData.numberOfRandomFacts(), times);
        }

        String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());

        SmartHomeMonitorBenchmark.writeToCsv(benchmarkResults,
                String.format("%s/data/%s_RETE_SmartHouse.csv",
                        System.getProperty("user.dir"), timestamp));

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}