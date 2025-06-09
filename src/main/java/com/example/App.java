package com.example;

import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "evrete-smarhouse", mixinStandardHelpOptions = true)
public class App implements Runnable {
    @Option(names = "--matches", description = "The maximum number of matches the smart house actor should perform", defaultValue = "25")
    private int matches;

    @Option(names = "--heavyGuard", description = "Whether to use a heavy guard", defaultValue = "false")
    private boolean heavyGuard;

    @Option(names = "--minParam", description = "The minimum parameter value, default 0", defaultValue = "0")
    private int minParam;

    @Option(names = "--paramStep", description = "The step by which the parameter value should increase, default 1", defaultValue = "1")
    private int paramStep;

    @Option(names = "--maxParam", description = "The maximum parameter value, default 20", defaultValue = "20")
    private int maxParam;

    @Option(names = "--path", description = "The folder path to which to write the benchmark results, default \"data\"", defaultValue = "data")
    private String path;

    static void runBenchmarks(List<BenchmarkData> benchmarkData, Integer warmupRepetitions, Integer repetitions, boolean isGuardDelayed, String outputDataDir) {
        SmartHomeMonitorBenchmark benchmark = new SmartHomeMonitorBenchmark(benchmarkData, warmupRepetitions, repetitions, isGuardDelayed);

        benchmark.runWarmup();

        benchmark.runBenchmark(outputDataDir);
    }

    @Override
    public void run() {
        System.out.println("Running simple smart house benchmark with minParam = " + minParam + ", paramStep = " + paramStep +
                ", maxParam = " + maxParam + ", matches = " + matches + ", heavyGuard = " + heavyGuard);

        List<BenchmarkData> benchmarkData = Utils.generateBenchmarkData(matches, minParam, paramStep, maxParam);

        runBenchmarks(benchmarkData, 0, 1, heavyGuard, path);
    }

    public static void main(String[] args) {
        new CommandLine(new App()).execute(args);
    }
}
