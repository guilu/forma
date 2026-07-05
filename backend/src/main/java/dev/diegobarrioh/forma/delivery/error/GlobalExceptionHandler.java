package dev.diegobarrioh.forma.delivery.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

  /** Header carrying a request correlation id; populated end-to-end by FOR-91. */
  static final String CORRELATION_HEADER = "X-Correlation-Id";

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

  private String correlationId(HttpServletRequest request) {
    return request == null ? null : request.getHeader(CORRELATION_HEADER);
  }
}
