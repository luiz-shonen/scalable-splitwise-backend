package com.splitwise.exception;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("Not found");
        ResponseEntity<Map<String, Object>> response = handler.handleEntityNotFound(ex);
        
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertEquals("Not Found", response.getBody().get("error"));
        Assertions.assertEquals("Not found", response.getBody().get("message"));
    }

    @Test
    void handleValidationException() {
        ValidationException ex = new ValidationException("Invalid data");
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(ex);
        
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Bad Request", response.getBody().get("error"));
        Assertions.assertEquals("Invalid data", response.getBody().get("message"));
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("Internal error");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);
        
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals("Internal Server Error", response.getBody().get("error"));
        Assertions.assertEquals("Internal error", response.getBody().get("message"));
    }
}
