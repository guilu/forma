package dev.diegobarrioh.forma.delivery.error;

import dev.diegobarrioh.forma.application.ForbiddenException;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.OAuthStateException;
import dev.diegobarrioh.forma.application.ProviderOAuthException;
import dev.diegobarrioh.forma.application.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

  /**
   * A required {@code @RequestParam} is missing (FOR-127, e.g. {@code ?date=} on {@code
   * /nutrition/consumption}) — maps to 400 {@code VALIDATION_ERROR} rather than the framework's
   * default problem-detail body, keeping every endpoint's error shape consistent (ADR-005).
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMissingParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR,
        "Missing required parameter: " + ex.getParameterName(),
        correlationId(request),
        null);
  }

  /**
   * A {@code @RequestParam} or {@code @PathVariable} value doesn't match its target type (FOR-127,
   * e.g. a malformed {@code date}) — maps to 400 {@code VALIDATION_ERROR}, mirroring {@link
   * #handleMalformedRequestBody}'s reasoning for the request-body case.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR,
        "Invalid value for parameter: " + ex.getName(),
        correlationId(request),
        null);
  }

  /**
   * Application-layer validation failures outside {@code @Valid} request bodies — e.g. an unknown
   * path-variable enum value such as an integration {@code provider} (FOR-126) — map to 400 {@code
   * VALIDATION_ERROR} with the safe, caller-provided message, mirroring {@link #handleNotFound}.
   */
  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidationException(ValidationException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR, ex.getMessage(), correlationId(request), null);
  }

  /**
   * A multipart upload exceeding {@code spring.servlet.multipart.max-file-size}/{@code
   * max-request-size} (FOR-140: "Oversized... upload -> 400 VALIDATION_ERROR") maps to 400 rather
   * than the framework's default — Spring's multipart resolver can reject an oversized file before
   * it ever reaches {@code ProgressPhotoService}'s own application-level size check, so both layers
   * must map to the same predictable, safe error shape (ADR-005).
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR,
        "Uploaded file exceeds the maximum allowed size",
        correlationId(request),
        null);
  }

  /** Missing resources map to 404 with the safe, caller-provided message. */
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(NotFoundException ex, HttpServletRequest request) {
    return ApiError.of(ApiErrorCode.NOT_FOUND, ex.getMessage(), correlationId(request), null);
  }

  /**
   * A resource exists but belongs to another owner (FOR-140, ADR-002: reject cross-user access even
   * in the single-user MVP) maps to 403 with the safe, caller-provided message. First real use of
   * the {@link ApiErrorCode#FORBIDDEN} code reserved by FOR-88.
   */
  @ExceptionHandler(ForbiddenException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiError handleForbidden(ForbiddenException ex, HttpServletRequest request) {
    return ApiError.of(ApiErrorCode.FORBIDDEN, ex.getMessage(), correlationId(request), null);
  }

  /**
   * An OAuth callback's {@code state} did not match a stored, unexpired, unconsumed challenge
   * (FOR-131 Edge Cases: "mismatched/expired/replayed state → reject") — maps to 400 {@code
   * VALIDATION_ERROR}. {@link OAuthStateException#getMessage()} is always a fixed, generic string
   * (never the caller-supplied {@code state} value), so it is safe to return as-is (spec FOR-131
   * api.md: "Never log or echo code, state, or any token").
   */
  @ExceptionHandler(OAuthStateException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleOAuthState(OAuthStateException ex, HttpServletRequest request) {
    return ApiError.of(
        ApiErrorCode.VALIDATION_ERROR, ex.getMessage(), correlationId(request), null);
  }

  /**
   * A provider OAuth call (Withings token exchange/refresh) failed (FOR-131 Edge Cases: "Token
   * exchange failure... readable outcome, no secret leak") — maps to 502. {@link
   * ProviderOAuthException#getMessage()} is always a safe, generic summary the adapter constructs
   * deliberately without the raw provider response body, an authorization code, or a token (see its
   * javadoc), so it is safe to return as-is.
   */
  @ExceptionHandler(ProviderOAuthException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public ApiError handleProviderOAuth(ProviderOAuthException ex, HttpServletRequest request) {
    return ApiError.of(ApiErrorCode.PROVIDER_ERROR, ex.getMessage(), correlationId(request), null);
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
