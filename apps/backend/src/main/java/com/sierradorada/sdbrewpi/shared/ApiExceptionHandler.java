package com.sierradorada.sdbrewpi.shared;

import com.sierradorada.sdbrewpi.auth.UnauthorizedException;
import com.sierradorada.sdbrewpi.auth.InvalidCredentialsException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("INVALID_CREDENTIALS", exception.getMessage(), Instant.now(), Map.of()));
    }
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("UNAUTHORIZED", exception.getMessage(), Instant.now(), Map.of()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "La solicitud contiene valores inválidos", Instant.now(), fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_COMMAND", exception.getMessage(), Instant.now(), Map.of()));
    }

    @ExceptionHandler(RevisionConflictException.class)
    ResponseEntity<ApiError> conflict(RevisionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError("REVISION_CONFLICT", exception.getMessage(), Instant.now(), Map.of()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> stateConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError("STATE_CONFLICT", exception.getMessage(), Instant.now(), Map.of()));
    }

    @ExceptionHandler(TankNotFoundException.class)
    ResponseEntity<ApiError> notFound(TankNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("TANK_NOT_FOUND", exception.getMessage(), Instant.now(), Map.of()));
    }
}
