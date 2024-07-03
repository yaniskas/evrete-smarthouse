package com.example;

import java.util.List;

import com.example.MsgTypes.Action;

public record BenchmarkData(int numberOfFacts, int numberOfRandomFacts, List<Action> facts) {

    @Override
    public String toString() {
        return String.format("BenchmarkData{numberOfFacts=%d, numberOfRandomFacts=%d, facts=%s}",
            numberOfFacts, numberOfRandomFacts, facts);
    }

}
