package com.ams.resident.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
    }

    @Test
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resident profile not found");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getError().getCode());
        assertEquals("Resident profile not found", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleBadRequest() {
        BadRequestException ex = new BadRequestException("VALIDATION_ERROR", "Page index must not be negative");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        assertEquals("Page index must not be negative", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleConflict() {
        ConflictException ex = new ConflictException("EMAIL_ALREADY_IN_USE", "Email already in use");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMAIL_ALREADY_IN_USE", response.getBody().getError().getCode());
        assertEquals("Email already in use", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleValidationErrorsWithJoinedMessages() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must be valid email"));
        bindingResult.addError(new FieldError("target", "phone", "must not be blank"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethods()[0], -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("must be valid email"));
        assertTrue(response.getBody().getError().getMessage().contains("must not be blank"));
        assertTrue(response.getBody().getError().getMessage().contains(", "));
    }

    @Test
    void shouldHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Invalid JSON", new MockHttpInputMessage("bad".getBytes(StandardCharsets.UTF_8)));
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError().getCode());
        assertEquals("Malformed JSON request or invalid values", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getError().getCode());
        assertEquals("Forbidden", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleDependencyUnavailable() {
        DependencyUnavailableException ex = new DependencyUnavailableException("Identity service unavailable");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDependencyUnavailable(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEPENDENCY_UNAVAILABLE", response.getBody().getError().getCode());
        assertEquals("Identity service unavailable", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleRestClientException() {
        RestClientException ex = new RestClientException("Connection timed out");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleExternalServiceError(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEPENDENCY_UNAVAILABLE", response.getBody().getError().getCode());
        assertEquals("External service unavailable", response.getBody().getError().getMessage());
    }

    @Test
    void shouldHandleAllOtherExceptionsWithoutLeakingDetails() {
        Exception ex = new NullPointerException("NullPointer in InternalClass at com.ams.secret.Class.method()");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAllOtherExceptions(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError().getCode());
        assertEquals("An unexpected internal error occurred", response.getBody().getError().getMessage());
        assertFalse(response.getBody().getError().getMessage().contains("NullPointer"));
        assertFalse(response.getBody().getError().getMessage().contains("com.ams"));
    }
}
