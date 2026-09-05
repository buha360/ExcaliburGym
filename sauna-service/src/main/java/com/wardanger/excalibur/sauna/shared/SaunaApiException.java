package com.wardanger.excalibur.sauna.shared;

import org.springframework.http.HttpStatus;

public class SaunaApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public SaunaApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
