package com.rockandhardplaces.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    ResponseEntity<Map<String, Object>> notFound(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request, Map.of());
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, Object>> forbidden(SecurityException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", message(exception, "Forbidden"), request, Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> conflict(IllegalStateException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONFLICT", message(exception, "Request conflicts with current state"),
                request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
                request, fieldErrors);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message(exception, "Bad request"),
                request, Map.of());
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String error, String message,
            HttpServletRequest request, Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String message(Exception exception, String fallback) {
        return exception.getMessage() == null ? fallback : exception.getMessage();
    }
}
