package com.banking.forms.downstream.application;

/** Thrown for invalid downstream admin operations (unknown provider, bad config JSON). */
public class DownstreamException extends RuntimeException {

    public DownstreamException(String message) {
        super(message);
    }
}
