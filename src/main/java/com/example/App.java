package com.example;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.Action;
import com.google.gson.JsonArray;

public class App {

    static void runMonitor(List<Action> sampleData, boolean isGuardDelayed) throws InterruptedException, ExecutionException {
        Long startTime = null; 
        Long endTime = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var monitor = new MonitorMatcher(
                    new LinkedTransferQueue<Action>(),
                    new SmartHouseMonitor(isGuardDelayed));

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

    static void demo(boolean isGuardDelayed) {
        KnowledgeService service = new KnowledgeService();
        SmartHouseMonitor monitor = new SmartHouseMonitor(isGuardDelayed);
        Knowledge knowledge = monitor.createMonitorKnowledge(service);
        List<Action> facts = monitor.createSampleMonitorFacts();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            Measurement m = monitor.processFacts(knowledge, session, facts);
            System.out.println(m);
        }
        service.shutdown();
    }

    static void runBenchmarks(List<BenchmarkData> benchmarkData, Integer warmupRepetitions, Integer repetitions, boolean isGuardDelayed, String outputDataDir) {
        SmartHomeMonitorBenchmark benchmark = new SmartHomeMonitorBenchmark(benchmarkData, warmupRepetitions, repetitions, isGuardDelayed);

        benchmark.runWarmup();

        benchmark.runBenchmark(outputDataDir);
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Please provide the path to the smart house data file.");
            return;
        }

        String smartHouseDataFilePath = args[0];
        
        String outputDataDir = (args.length > 1) ? args[1] : "";
        if (outputDataDir.isEmpty()) {
            System.err.println("Please provide the path to the output data directory.");
        }

        boolean isGuardDelayed = (args.length > 2) && Boolean.parseBoolean(args[2]);

        var matches = 25;
        var heavyGuard = false;
        var minParam = 0;
        var paramStep = 1;
        var maxParam = 25;

        List<BenchmarkData> benchmarkData = Utils.generateBenchmarkData(matches, minParam, paramStep, maxParam);

        runBenchmarks(benchmarkData, 0, 1, heavyGuard, outputDataDir);
    }
}
