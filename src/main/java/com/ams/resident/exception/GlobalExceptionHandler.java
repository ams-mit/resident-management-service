package com.ams.resident.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        return new ResponseEntity<>(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        String code = ex.getCode() != null ? ex.getCode() : "BAD_REQUEST";
        return new ResponseEntity<>(ApiErrorResponse.of(code, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String code = ex.getCode() != null ? ex.getCode() : "CONFLICT";
        return new ResponseEntity<>(ApiErrorResponse.of(code, ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(message)) {
            message = "Validation failed";
        }
        return new ResponseEntity<>(ApiErrorResponse.of("VALIDATION_ERROR", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        String message = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(message)) {
            message = "Validation failed";
        }
        return new ResponseEntity<>(ApiErrorResponse.of("VALIDATION_ERROR", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ApiErrorResponse.of("VALIDATION_ERROR", "Malformed JSON request or invalid values"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ApiErrorResponse.of("FORBIDDEN", ex.getMessage() != null ? ex.getMessage() : "Access is denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleDependencyUnavailable(DependencyUnavailableException ex, HttpServletRequest request) {
        log.warn("Dependency unavailable: {}", ex.getMessage());
        return new ResponseEntity<>(ApiErrorResponse.of(ex.getCode(), ex.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalServiceError(RestClientException ex, HttpServletRequest request) {
        log.error("External service integration failure: {}", ex.getMessage());
        return new ResponseEntity<>(ApiErrorResponse.of("DEPENDENCY_UNAVAILABLE", "External service unavailable"), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return new ResponseEntity<>(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "An unexpected internal error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
