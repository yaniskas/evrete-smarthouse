package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import org.evrete.KnowledgeService;
import org.evrete.api.FactHandle;
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
        
    }
}
