package spring.abtechzone.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.dto.ApiResult;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResult<?>> handlingMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getFieldError();
        String enumKey = fieldError != null ? fieldError.getDefaultMessage() : null;
        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        if (enumKey != null) {
            try {
                errorCode = ErrorCode.valueOf(enumKey);
            } catch (IllegalArgumentException e) {
                log.debug("Field error message '{}' does not map to ErrorCode enum", enumKey);
            }
        }

        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.badRequest().body(apiResult);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    ResponseEntity<ApiResult<?>> handlingHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.badRequest().body(apiResult);
    }

    @ExceptionHandler(value = MissingRequestHeaderException.class)
    ResponseEntity<ApiResult<?>> handlingMissingRequestHeaderException(MissingRequestHeaderException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.badRequest().body(apiResult);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResult<?>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResult);
    }

    @ExceptionHandler(value = CheckoutChangedException.class)
    ResponseEntity<ApiResult<?>> handlingCheckoutChangedException(CheckoutChangedException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(exception.getLatestReview())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResult);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResult<?>> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResult.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    ResponseEntity<ApiResult<?>> handlingNoResourceFoundException(NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.ORDER_NOT_FOUND;
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResult.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AuthenticationException.class)
    ResponseEntity<ApiResult<?>> handlingAuthenticationException(AuthenticationException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResult.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    ResponseEntity<ApiResult<?>> handlingDataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.error("Data integrity violation encountered", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResult<?>> handlingRuntimeException(Exception exception) {
        log.error("Uncaught server exception encountered", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResult<?> apiResult = ApiResult.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResult);
    }
}
