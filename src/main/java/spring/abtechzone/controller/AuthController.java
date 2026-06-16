package spring.abtechzone.controller;

import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.abtechzone.dto.ApiResponse;
import spring.abtechzone.dto.auth.*;
import spring.abtechzone.service.AuthService;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/token")
    ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest request){
        var result = authService.authenticate(request);

        return ApiResponse.<AuthResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authService.introspect(request);

        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
       authService.logout(request);

        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        var result = authService.refreshToken(request);

        return ApiResponse.<AuthResponse>builder()
                .result(result)
                .build();
    }

}
