package dev.diegobarrioh.forma.delivery.error;

import dev.diegobarrioh.forma.application.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Baseline API exception handling (FOR-88, ADR-005). Maps exceptions to the consistent {@link
 * ApiError} shape and guarantees clients never receive stack traces or internal exception messages.
 * Full detail is logged server-side only, keyed by correlation id.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Header carrying a request correlation id (fallback when the MDC is not set). */
  static final String CORRELATION_HEADER = "X-Correlation-Id";

  /** MDC key populated per request by the CorrelationIdFilter (FOR-91). */
  static final String CORRELATION_MDC_KEY = "correlationId";

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Bean-validation failures on {@code @Valid} request bodies map to 400. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiError.FieldValidationError> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldValidationError(fe.getField(), fe.getDefaultMessage()))
            .toList();
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR,
        "Request validation failed",
        correlationId(request),
        details);
  }

  /**
   * Malformed JSON, or a value that doesn't match its target type (e.g. a non-numeric string for a
   * numeric field), maps to 400 {@code VALIDATION_ERROR} instead of leaking as a 500 (ADR-005:
   * predictable, safe errors; needed for FOR-125's "non-numeric target" edge case, and generically
   * correct for every endpoint since a malformed body is always a caller input error, never a
   * server fault).
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMalformedRequestBody(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR, "Malformed request body", correlationId(request), null);
  }

  /** Missing resources map to 404 with the safe, caller-provided message. */
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(NotFoundException ex, HttpServletRequest request) {
    return ApiError.of(ApiErrorCode.NOT_FOUND, ex.getMessage(), correlationId(request), null);
  }

  /**
   * Catch-all for unexpected errors. Logs the full exception server-side and returns a safe,
   * generic body — no stack trace, no internal message.
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
    String correlationId = correlationId(request);
    log.error("Unhandled exception [correlationId={}]", correlationId, ex);
    return ApiError.of(
        ApiErrorCode.INTERNAL_ERROR, "An unexpected error occurred", correlationId, null);
  }

  /**
   * Prefer the correlation id assigned by the CorrelationIdFilter (MDC); fall back to the request
   * header when no filter ran (e.g. isolated tests).
   */
  private String correlationId(HttpServletRequest request) {
    String fromMdc = MDC.get(CORRELATION_MDC_KEY);
    if (fromMdc != null && !fromMdc.isBlank()) {
      return fromMdc;
    }
    return request == null ? null : request.getHeader(CORRELATION_HEADER);
  }
}
