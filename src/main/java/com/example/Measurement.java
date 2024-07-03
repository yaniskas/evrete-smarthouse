package com.example;

import java.time.Duration;

public record Measurement(Duration time) {

    @Override
    public String toString() {
        return String.format("Measurement{time=%d (ms)}", time.toMillis());
    }
}
