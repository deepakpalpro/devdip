package com.banking.forms.processing.application;

/** Raised when a review action is not valid for a submission's current status. */
public class ReviewException extends RuntimeException {

    public ReviewException(String message) {
        super(message);
    }
}
