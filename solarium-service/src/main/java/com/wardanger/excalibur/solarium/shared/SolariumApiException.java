package com.wardanger.excalibur.solarium.shared;

import org.springframework.http.HttpStatus;

public class SolariumApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public SolariumApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
