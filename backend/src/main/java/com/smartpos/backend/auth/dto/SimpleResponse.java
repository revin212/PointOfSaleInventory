package com.smartpos.backend.auth.dto;

public record SimpleResponse(boolean success) {
    public static SimpleResponse ok() {
        return new SimpleResponse(true);
    }
}
