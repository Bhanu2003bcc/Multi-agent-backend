package com.research.backend.client;

public class AgentClientException extends RuntimeException {
    private final int statusCode;

    public AgentClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
