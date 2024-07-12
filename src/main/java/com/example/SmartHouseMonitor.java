package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evrete.KnowledgeService;
import org.evrete.api.FactHandle;
import org.evrete.api.IntToValue;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import com.example.MsgTypes.*;

public class SmartHouseMonitor {

    private boolean isGuardDelayed;

    boolean validateTimestampsOrder(Date timestamp1, Date timestamp2, Date timestamp3) {

        return (timestamp1.before(timestamp2) || timestamp1.equals(timestamp2)) &&
                (timestamp2.before(timestamp3) || timestamp2.equals(timestamp3));

    }
    static void busyLoop() {
        long start = System.nanoTime();
        while (System.nanoTime() - start < 100000) { }
    }

    boolean bathroomOccupied(IntToValue values) {
        Action.Motion motion = (Action.Motion) values.get(0);
        Action.AmbientLight ambientLight = (Action.AmbientLight) values.get(1);
        Action.Light light = (Action.Light) values.get(2);

        List<String> rooms = List.of(motion.room(), ambientLight.room(), light.room());
        List<Date> timestamps = List.of(motion.timestamp(), ambientLight.timestamp(), light.timestamp());

        if (isGuardDelayed) {
            busyLoop();
        }
        return validateTimestampsOrder(timestamps.get(0), timestamps.get(1), timestamps.get(2))
                && rooms.stream().allMatch(room -> room.equals("bathroom"))
                && motion.status()
                && !light.status()
                && ambientLight.lightLevel() <= 40;
    }

    public SmartHouseMonitor(boolean isGuardDelayed) {
        super();
        this.isGuardDelayed = isGuardDelayed;
    }

    List<List<Integer>> rule1Matches = new ArrayList<>();

    public Knowledge createMonitorKnowledge(KnowledgeService service) {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .newRule("E1")
                .forEach(
                        "$motion", Action.Motion.class,
                        "$ambientLight", Action.AmbientLight.class,
                        "$light", Action.Light.class)
                .where(this::bathroomOccupied, "$motion", "$ambientLight", "$light")
                .execute(ctx -> {
                    Action.Motion motion = ctx.get("$motion");
                    Action.AmbientLight ambientLight = ctx.get("$ambientLight");
                    Action.Light light = ctx.get("$light");
                    rule1Matches.add(List.of(motion.id(), ambientLight.id(), light.id()));
                }).build();
        return knowledge;
    }

    public List<Action> createSampleMonitorFacts() {
        List<Action> facts = new ArrayList<>();
        facts.add(new Action.Motion(0, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(1, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(2, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(3, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(4, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(5, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(6, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(7, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(8, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(9, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(10, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(11, 39, "bathroom", new Date()));

        facts.add(new Action.Motion(12, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(13, 39, "bathroom", new Date()));

        facts.add(new Action.Light(14, false, "bathroom", new Date()));
        return facts;
    }

    private Map<Integer, FactHandle> factHandles = new HashMap<>();

    public Map<Integer, FactHandle> getFactHandles() {
        return factHandles;
    }

    public void insertFact(Action fact, StatefulSession session) {
        switch (fact) {
            case Action.Motion motion -> {
                FactHandle handle = session.insert(motion);
                if (handle != null) {
                    factHandles.put(motion.id(), handle);
                }
            }
            case Action.AmbientLight ambientLight -> {
                FactHandle handle = session.insert(ambientLight);
                if (handle != null) {
                    factHandles.put(ambientLight.id(), handle);
                }
            }
            case Action.Light light -> {
                FactHandle handle = session.insert(light);
                if (handle != null) {
                    factHandles.put(light.id(), handle);
                }
            }
            case Action.Contact contact -> {
                FactHandle handle = session.insert(contact);
                if (handle != null) {
                    factHandles.put(contact.id(), handle);
                }
            }
            case Action.Consumption consumption -> {
                FactHandle handle = session.insert(consumption);
                if (handle != null) {
                    factHandles.put(consumption.id(), handle);
                }
            }
            case Action.HeatingF heatingF -> {
                FactHandle handle = session.insert(heatingF);
                if (handle != null) {
                    factHandles.put(heatingF.id(), handle);
                }
            }
            case Action.DoorBell doorBell -> {
                FactHandle handle = session.insert(doorBell);
                if (handle != null) {
                    factHandles.put(doorBell.id(), handle);
                }
            }
            case Action.ShutOff shutOff -> {
                FactHandle handle = session.insert(shutOff);
                if (handle != null) {
                    factHandles.put(-1, handle);
                }
            }
        }
    }

    Comparator<List<Integer>> lexComparator = new LexComparator();

    public List<Integer> matchFact(Knowledge knowledge, StatefulSession session, Action fact) {
        List<List<Integer>> matchesAcrossRules = new ArrayList<>();
        List<Integer> selectedMatch = new ArrayList<>();
        insertFact(fact, session);
        session.fire();
        if (rule1Matches.size() > 0) {
            Collections.sort(rule1Matches, lexComparator);
                 
            List<Integer> match = rule1Matches.get(0);
            matchesAcrossRules.add(match);
            rule1Matches.remove(match);
        }

        if (matchesAcrossRules.size() > 0) {
            Collections.sort(matchesAcrossRules, lexComparator);
            selectedMatch = matchesAcrossRules.get(0);
            // Clean up
            session.delete(factHandles.get(selectedMatch.get(0)));
            session.delete(factHandles.get(selectedMatch.get(1)));
            session.delete(factHandles.get(selectedMatch.get(2)));

            factHandles.remove(selectedMatch.get(0));
            factHandles.remove(selectedMatch.get(1));
            factHandles.remove(selectedMatch.get(2));

            rule1Matches.clear();
        }

        matchesAcrossRules.clear();
        return selectedMatch;
    }

    public Measurement processFacts(Knowledge knowledge, StatefulSession session, List<Action> facts) {
        Long startTime = null;
        Long endTime = null;
        List<List<Integer>> matches = new ArrayList<>();
        startTime = System.currentTimeMillis();
        for (Action fact : facts) {
            List<Integer> matchFound = matchFact(knowledge, session, fact);
            if (matchFound.size() > 0) {
                matches.add(matchFound);
            }
        }

        endTime = System.currentTimeMillis();

        // System.out.println("Matches: " + matches);

        return new Measurement(Duration.ofMillis(endTime - startTime));
    }
}