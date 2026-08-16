package spring.abtechzone.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import spring.abtechzone.common.dto.ApiResult;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName(
            "handlingMethodArgumentNotValidException: returns 400 with mapped ErrorCode when field message matches enum")
    void methodArgumentNotValid_enumMatch_returns400WithErrorCode() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("userCreationRequest", "password", "PASSWORD_INVALID");
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.PASSWORD_INVALID.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.PASSWORD_INVALID.getMessage());
    }

    @Test
    @DisplayName(
            "handlingMethodArgumentNotValidException: returns 400 with INVALID_KEY when field message does not match enum")
    void methodArgumentNotValid_noEnumMatch_returns400WithInvalidKey() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("userCreationRequest", "email", "must be a well-formed email address");
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_KEY.getCode());
    }

    @Test
    @DisplayName("handlingHttpMessageNotReadableException: returns 400 with INVALID_KEY")
    void httpMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingHttpMessageNotReadableException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_KEY.getCode());
    }

    @Test
    @DisplayName("handlingAppException: returns status code defined in ErrorCode (e.g. 503 for SYSTEM_BUSY)")
    void appException_systemBusy_returns503() {
        AppException ex = new AppException(ErrorCode.SYSTEM_BUSY);

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.SYSTEM_BUSY.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.SYSTEM_BUSY.getMessage());
    }

    @Test
    @DisplayName("handlingAppException: returns status code defined in ErrorCode (e.g. 500 for SYSTEM_ERROR)")
    void appException_systemError_returns500() {
        AppException ex = new AppException(ErrorCode.SYSTEM_ERROR);

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.SYSTEM_ERROR.getMessage());
    }

    @Test
    @DisplayName("handlingAccessDeniedException: returns 403 Forbidden")
    void accessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("handlingAuthenticationException: returns 401 Unauthorized")
    void authenticationException_returns401() {
        AuthenticationException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingAuthenticationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED.getCode());
    }

    @Test
    @DisplayName("handlingDataIntegrityViolationException: returns 400 without leaking SQL or constraint details")
    void dataIntegrityViolation_returns400WithoutDetails() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"idx_user_email\"\nDETAIL: Key (email)=(test@example.com) already exists.");

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingDataIntegrityViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        assertThat(response.getBody().getMessage()).doesNotContain("idx_user_email");
        assertThat(response.getBody().getMessage()).doesNotContain("duplicate key");
    }

    @Test
    @DisplayName("handlingRuntimeException: returns 500 without leaking internal exception message")
    void uncaughtException_returns500WithoutInternalMessage() {
        RuntimeException ex = new NullPointerException("internal null pointer in database query engine");

        ResponseEntity<ApiResult<?>> response = exceptionHandler.handlingRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        assertThat(response.getBody().getMessage()).doesNotContain("internal null pointer");
    }
}
