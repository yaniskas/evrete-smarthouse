package com.example;

import java.time.Duration;
import java.util.ArrayList;
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

    boolean validateTimestampsOrder(Date timestamp1, Date timestamp2, Date timestamp3) {

        return (timestamp1.before(timestamp2) || timestamp1.equals(timestamp2)) &&
                (timestamp2.before(timestamp3) || timestamp2.equals(timestamp3));

    }

    boolean bathroomOccupied(IntToValue values) {
        Action.Motion motion = (Action.Motion) values.get(0);
        Action.AmbientLight ambientLight = (Action.AmbientLight) values.get(1);
        Action.Light light = (Action.Light) values.get(2);

        List<String> rooms = List.of(motion.room(), ambientLight.room(), light.room());
        List<Date> timestamps = List.of(motion.timestamp(), ambientLight.timestamp(), light.timestamp());

        return validateTimestampsOrder(timestamps.get(0), timestamps.get(1), timestamps.get(2))
                && rooms.stream().allMatch(room -> room.equals("bathroom"))
                && motion.status()
                && !light.status()
                && ambientLight.lightLevel() <= 40;
    }

    boolean occupiedHome(IntToValue values) {
        Action.Motion motion1 = (Action.Motion) values.get(0);
        Action.Contact contact = (Action.Contact) values.get(1);
        Action.Motion motion2 = (Action.Motion) values.get(2);

        List<Boolean> statuses = List.of(motion1.status(), contact.status(), motion2.status());
        List<Date> timestamps = List.of(motion1.timestamp(), contact.timestamp(), motion2.timestamp());

        return motion1.id() != motion2.id()
                && validateTimestampsOrder(timestamps.get(0), timestamps.get(1), timestamps.get(2))
                && statuses.stream().allMatch(status -> status)
                && motion1.room().equals("front_door")
                && contact.room().equals("front_door")
                && motion2.room().equals("entrance_hall");
    }

    boolean emptyHome(IntToValue values) {
        Action.Motion motion1 = (Action.Motion) values.get(0);
        Action.Contact contact = (Action.Contact) values.get(1);
        Action.Motion motion2 = (Action.Motion) values.get(2);

        List<Boolean> statuses = List.of(motion1.status(), contact.status(), motion2.status());
        List<Date> timestamps = List.of(motion1.timestamp(), contact.timestamp(), motion2.timestamp());

        return motion1.id() != motion2.id()
                && validateTimestampsOrder(timestamps.get(0), timestamps.get(1), timestamps.get(2))
                && statuses.stream().allMatch(status -> status)
                && motion1.room().equals("entrance_hall")
                && contact.room().equals("front_door")
                && motion2.room().equals("front_door");
    }

    List<List<Integer>> rule1Matches = new ArrayList<>();
    List<List<Integer>> rule2Matches = new ArrayList<>();
    List<List<Integer>> rule3Matches = new ArrayList<>();

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
                })
                .build()
                .builder()
                .newRule("E5.a")
                .forEach(
                        "$motion1", Action.Motion.class,
                        "$contact", Action.Contact.class,
                        "$motion2", Action.Motion.class)
                .where(this::occupiedHome, "$motion1", "$contact", "$motion2")
                .execute(ctx -> {
                    Action.Motion motion1 = ctx.get("$motion1");
                    Action.Contact contact = ctx.get("$contact");
                    Action.Motion motion2 = ctx.get("$motion2");
                    rule2Matches.add(List.of(motion1.id(), contact.id(), motion2.id()));
                })
                .build()
                .builder()
                .newRule("E5.b")
                .forEach(
                        "$motion1", Action.Motion.class,
                        "$contact", Action.Contact.class,
                        "$motion2", Action.Motion.class)
                .where(this::emptyHome, "$motion1", "$contact", "$motion2")
                .execute(ctx -> {
                    Action.Motion motion1 = ctx.get("$motion1");
                    Action.Contact contact = ctx.get("$contact");
                    Action.Motion motion2 = ctx.get("$motion2");

                    rule3Matches.add(List.of(motion1.id(), contact.id(), motion2.id()));
                })
                .build();
                // .builder()
                // .newRule("ShutOff")
                // .forEach(
                //         "$shutOff", Action.ShutOff.class)
                // .execute(ctx -> )

        return knowledge;
    }

    public List<Action> createSampleMonitorFacts() {
        List<Action> facts = new ArrayList<>();
        facts.add(new Action.Motion(0, true, "bathroom", new Date()));
        facts.add(new Action.AmbientLight(1, 39, "bathroom", new Date()));
        facts.add(new Action.Light(2, false, "bathroom", new Date()));

        facts.add(new Action.Motion(3, true, "front_door", new Date()));
        facts.add(new Action.Contact(4, true, "front_door", new Date()));
        facts.add(new Action.Motion(5, true, "entrance_hall", new Date()));

        facts.add(new Action.Motion(6, true, "entrance_hall", new Date()));
        facts.add(new Action.Contact(7, true, "front_door", new Date()));
        facts.add(new Action.Motion(8, true, "front_door", new Date()));

        // facts.add(new Action.Motion(9, true, "fsdagsfgsfg", new Date()));
        // facts.add(new Action.AmbientLight(10, 39, "fsdagsfgsfg", new Date()));
        // facts.add(new Action.Light(11, false, "fsdagsfgsfg", new Date()));

        // facts.add(new Action.Motion(12, true, "gdsfhdfhdsh", new Date()));
        // facts.add(new Action.Contact(13, true, "gdsfhdfhdsh", new Date()));
        // facts.add(new Action.Motion(14, true, "gdsrgdsh", new Date()));

        // facts.add(new Action.Motion(15, true, "gdsrgdsh", new Date()));
        // facts.add(new Action.Contact(16, true, "gdsfhdfhdsh", new Date()));
        // facts.add(new Action.Motion(17, true, "gdsfhdfhdsh", new Date()));

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
    
    public List<Integer> matchFact(Knowledge knowledge, StatefulSession session, Action fact) {
        List<List<Integer>> matchesAcrossRules = new ArrayList<>();
        List<Integer> selectedMatch = new ArrayList<>();
        insertFact(fact, session);
        session.fire();
        if (rule1Matches.size() > 0) {
            List<Integer> match = rule1Matches.get(0);
            matchesAcrossRules.add(match);
            rule1Matches.remove(match);
        }
        if (rule2Matches.size() > 0) {
            List<Integer> match = rule2Matches.get(0);
            matchesAcrossRules.add(match);
            rule2Matches.remove(match);
        }
        if (rule3Matches.size() > 0) {
            List<Integer> match = rule3Matches.get(0);
            matchesAcrossRules.add(match);
            rule3Matches.remove(match);
        }
        if (matchesAcrossRules.size() > 0) {
            selectedMatch = matchesAcrossRules.get(0);

            session.delete(factHandles.get(selectedMatch.get(0)));
            session.delete(factHandles.get(selectedMatch.get(1)));
            session.delete(factHandles.get(selectedMatch.get(2)));
            // Clean up
        }
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

        System.out.println("Matches: " + matches.size());

        return new Measurement(Duration.ofMillis(endTime - startTime));
    }
}