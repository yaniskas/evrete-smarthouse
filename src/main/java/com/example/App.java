package com.example;

import java.util.List;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.Action;
import com.google.gson.JsonArray;

public class App {
  public static void main(String[] args) {
    String smartHouseDataStr = Utils
        .readFileAsJsonStr("src/main/java/com/example/MonitorData/2024_07_02_13_53_22_smartHouseData.json");

    JsonArray smartHouseData = Utils.parseJson(smartHouseDataStr);

    List<BenchmarkData> benchmarkData = Utils.parseMessagesRows(smartHouseData);

    System.out.println("Messages rows: " + benchmarkData.get(0).numberOfFacts());


    KnowledgeService service = new KnowledgeService();
    SmartHouseMonitor monitor = new SmartHouseMonitor();
    Knowledge knowledge = monitor.createMonitorKnowledge(service);
    List<Action> facts = benchmarkData.get(0).facts(); // monitor.createSampleMonitorFacts();

    try (StatefulSession session = knowledge.newStatefulSession()) {
      Measurement m = monitor.processFacts(knowledge, session, facts);
      System.out.println(m);
    }
    service.shutdown();

    // Measurement measurement = monitor.matchFacts(knowledge, facts);
    // System.out.println(measurement);
    // service.shutdown();

    SmartHomeMonitorBenchmark benchmark = 
        new SmartHomeMonitorBenchmark(benchmarkData, 1, 1);
        
    
    benchmark.runWarmup();

    benchmark.runBenchmark();
        

  }
}
