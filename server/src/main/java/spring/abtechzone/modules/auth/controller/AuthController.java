package spring.abtechzone.modules.auth.controller;

import java.text.ParseException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.auth.dto.request.AuthRequest;
import spring.abtechzone.modules.auth.dto.request.IntrospectRequest;
import spring.abtechzone.modules.auth.dto.request.LogoutRequest;
import spring.abtechzone.modules.auth.dto.request.RefreshTokenRequest;
import spring.abtechzone.modules.auth.dto.response.AuthResponse;
import spring.abtechzone.modules.auth.dto.response.IntrospectResponse;
import spring.abtechzone.modules.auth.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "Endpoints for user authentication, token management, and session control")
public class AuthController {

    AuthService authService;

    @Operation(
            summary = "Sign in",
            description = "Authenticate user credentials and return a JWT access token and refresh token")
    @SecurityRequirement(name = "")
    @PostMapping("/sign-in")
    ApiResult<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        var result = authService.authenticate(request);

        return ApiResult.<AuthResponse>builder().result(result).build();
    }

    @PostMapping("/introspect")
    @Operation(summary = "Introspect token", description = "Validate a JWT token and check whether it is still active")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    @SecurityRequirement(name = "")
    ApiResult<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authService.introspect(request);

        return ApiResult.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/sign-out")
    @Operation(
            summary = "Sign out",
            description = "Invalidate the current JWT by adding it to the token blacklist in Redis")
    @ApiResponse(responseCode = "200", description = "Signed out successfully")
    @ApiResponse(responseCode = "401", description = "Token is invalid")
    ApiResult<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authService.logout(request);

        return ApiResult.<Void>builder().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new JWT access token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    @SecurityRequirement(name = "")
    ApiResult<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        var result = authService.refreshToken(request);

        return ApiResult.<AuthResponse>builder().result(result).build();
    }
}
