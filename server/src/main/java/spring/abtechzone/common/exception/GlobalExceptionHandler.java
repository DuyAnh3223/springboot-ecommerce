package spring.abtechzone.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import spring.abtechzone.common.dto.ApiResult;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResult> handlingMethodArgumentNotValidException(MethodArgumentNotValidException exception) {

        String enumKey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
        }

        ApiResult apiResult = new ApiResult();

        apiResult.setCode(errorCode.getCode());
        apiResult.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResult);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResult> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResult apiResult = new ApiResult();
        apiResult.setCode(errorCode.getCode());
        apiResult.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResult);
    }

    // Nếu có exception khác ngoài các exception
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResult> handlingRuntimeException(Exception exception) {
        ApiResult apiResult = new ApiResult();
        apiResult.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResult.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResult);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResult> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResult.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
