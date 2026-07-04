package com.banking.forms.app.config;

import com.banking.forms.formdefinition.application.FormConflictException;
import com.banking.forms.formdefinition.application.FormNotFoundException;
import com.banking.forms.formdefinition.application.FormSchemaException;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.application.FormImportNotFoundException;
import com.banking.forms.notification.application.NotificationException;
import com.banking.forms.downstream.application.DownstreamException;
import com.banking.forms.serviceintegration.application.ServiceIntegrationException;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.application.SubmissionValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubmissionValidationException.class)
    ResponseEntity<SimpleError> handleValidation(SubmissionValidationException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    ResponseEntity<SimpleError> handleNotFound(SubmissionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(FormNotFoundException.class)
    ResponseEntity<SimpleError> handleFormNotFound(FormNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(FormConflictException.class)
    ResponseEntity<SimpleError> handleFormConflict(FormConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(FormSchemaException.class)
    ResponseEntity<SimpleError> handleFormSchema(FormSchemaException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(FormImportNotFoundException.class)
    ResponseEntity<SimpleError> handleImportNotFound(FormImportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(FormImportException.class)
    ResponseEntity<SimpleError> handleImport(FormImportException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(NotificationException.class)
    ResponseEntity<SimpleError> handleNotification(NotificationException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(DownstreamException.class)
    ResponseEntity<SimpleError> handleDownstream(DownstreamException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    @ExceptionHandler(ServiceIntegrationException.class)
    ResponseEntity<SimpleError> handleServiceIntegration(ServiceIntegrationException ex) {
        return ResponseEntity.badRequest().body(new SimpleError(ex.getMessage()));
    }

    public record SimpleError(String message) {}
}
