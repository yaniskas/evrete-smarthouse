package com.example;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.stream.IntStream;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.Action;
import com.google.gson.JsonArray;

public class App {

    final static String SMART_HOUSE_DATA_FILE = "src/main/java/com/example/MonitorData/2024_07_02_13_53_22_smartHouseData.json";
    
    static String smartHouseDataStr = Utils.readFileAsJsonStr(SMART_HOUSE_DATA_FILE);
    static JsonArray smartHouseData = Utils.parseJson(smartHouseDataStr);
    static List<BenchmarkData> benchmarkData = Utils.parseBenchmarkData(smartHouseData);
    static List<Action> sampleData = benchmarkData.get(2).facts();

    static void runMonitor() throws InterruptedException, ExecutionException {
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
                for (int i = 0; i < sampleData.size(); i++) {
                    // System.out.println("Thread: " + Thread.currentThread());
                    monitor.send(sampleData.get(i));
                }
            });

            sender.get();
            monitor.send(new Action.ShutOff());
            monitorMatcher.get();
            endTime = System.currentTimeMillis();
            // end
        }
        
        System.out.println("Time: " + (endTime - startTime) + " ms");
    }

    static void demo() {
        KnowledgeService service = new KnowledgeService();
        SmartHouseMonitor monitor = new SmartHouseMonitor();
        Knowledge knowledge = monitor.createMonitorKnowledge(service);
        List<Action> facts = benchmarkData.get(0).facts();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            Measurement m = monitor.processFacts(knowledge, session, facts);
            System.out.println(m);
        }
        service.shutdown();

        // Measurement measurement = monitor.matchFacts(knowledge, facts);
        // System.out.println(measurement);
        // service.shutdown();
    }

    static void runBenchmarks(List<BenchmarkData> benchmarkData, Integer warmupRepetitions, Integer repetitions) {
        SmartHomeMonitorBenchmark benchmark = new SmartHomeMonitorBenchmark(benchmarkData, warmupRepetitions, repetitions);

        benchmark.runWarmup();

        benchmark.runBenchmark();
    }

    public static void main(String[] args) {
        // try {
        //     runMonitor();
        // } catch (InterruptedException | ExecutionException e) {
        //     e.printStackTrace();
        // }
        runBenchmarks(benchmarkData, 5, 5);
    }
}
