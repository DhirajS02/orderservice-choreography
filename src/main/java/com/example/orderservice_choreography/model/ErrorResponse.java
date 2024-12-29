package com.example.orderservice_choreography.model;

import java.time.Instant;

public record ErrorResponse(String message, String details, Instant timestamp) {

    public ErrorResponse(String message, String details) {
        this(message, details, Instant.now());
    }
}
