package dev.pmlsp.dict.adapter.web;

import dev.pmlsp.dict.adapter.web.dto.WebDtos.ProblemDetail;
import dev.pmlsp.dict.domain.exception.ClaimConflictException;
import dev.pmlsp.dict.domain.exception.ClaimNotFoundException;
import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.exception.DictUnavailableException;
import dev.pmlsp.dict.domain.exception.DuplicateKeyException;
import dev.pmlsp.dict.domain.exception.KeyNotFoundException;
import dev.pmlsp.dict.domain.exception.PolicyViolationException;
import dev.pmlsp.dict.domain.exception.RateLimitedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://dict-client-reference/problems";

    @ExceptionHandler(KeyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(KeyNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "KEY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(ClaimNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "CLAIM_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> handle(DuplicateKeyException ex) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_KEY", ex.getMessage());
    }

    @ExceptionHandler(ClaimConflictException.class)
    public ResponseEntity<ProblemDetail> handle(ClaimConflictException ex) {
        return problem(HttpStatus.CONFLICT, "CLAIM_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ProblemDetail> handle(RateLimitedException ex) {
        HttpHeaders headers = new HttpHeaders();
        ex.retryAfter().ifPresent(d -> headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(d.toSeconds())));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(buildBody(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage()));
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ProblemDetail> handle(PolicyViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage());
    }

    @ExceptionHandler(DictUnavailableException.class)
    public ResponseEntity<ProblemDetail> handle(DictUnavailableException ex) {
        log.warn("dict.upstream.unavailable: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "UPSTREAM_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(DictException.class)
    public ResponseEntity<ProblemDetail> handleDictFallback(DictException ex) {
        log.warn("dict.unhandled: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "DICT_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("validation failed");
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handle(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(buildBody(status, code, detail));
    }

    private static ProblemDetail buildBody(HttpStatus status, String code, String detail) {
        return new ProblemDetail(
                TYPE_BASE + "/" + code.toLowerCase().replace('_', '-'),
                status.getReasonPhrase(),
                status.value(),
                detail,
                code);
    }
}
