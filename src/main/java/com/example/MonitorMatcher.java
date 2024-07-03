package com.example;

import java.util.concurrent.*;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.Action;

public class MonitorMatcher {
    
    private LinkedTransferQueue<Action> queue;
    private SmartHouseMonitor monitor;

    public SmartHouseMonitor getMonitor() {
        return monitor;
    }

    public LinkedTransferQueue<Action> getQueue() {
        return queue;
    }

    public void send(Action action) {
        queue.put(action);
    }

    public MonitorMatcher(LinkedTransferQueue<Action> queue, SmartHouseMonitor monitor) {
        this.queue = queue;
        this.monitor = monitor;
    }

    public void match() {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = monitor.createMonitorKnowledge(service);
        boolean isDone = false;
        try (StatefulSession session = knowledge.newStatefulSession()) {
            while (!isDone) {
                Action action = queue.take();
                switch (action) {
                    case Action.ShutOff s -> 
                        isDone = true;                
                    default -> 
                        {
                            // List<Integer> match = 
                            monitor.matchFact(knowledge, session, action);
                            // if (match.size() > 0) {
                            //     System.out.println("Match: " + match);
                            // }
                        }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }
}
