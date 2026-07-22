package spring.abtechzone.modules.user.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.user.dto.request.UserCreationRequest;
import spring.abtechzone.modules.user.dto.request.UserSearchRequest;
import spring.abtechzone.modules.user.dto.request.UserUpdateRequest;
import spring.abtechzone.modules.user.dto.response.UserResponse;
import spring.abtechzone.modules.user.service.UserService;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Users", description = "User registration, profile management, and admin user operations")
public class UserController {

    UserService userService;

    @PostMapping
    @Operation(
            summary = "Register user",
            description = "Create a new user account. This endpoint is public and does not require authentication")
    @ApiResponse(responseCode = "200", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Validation error or username/email already exists")
    ApiResult<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResult<UserResponse> apiResult = new ApiResult<>();
        apiResult.setResult(userService.createUser(request));

        return apiResult;
    }

    @GetMapping
    @Operation(
            summary = "Get users (paginated)",
            description = "Retrieve a paginated, searchable list of all users. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Page<UserResponse>> getUsers(@Valid UserSearchRequest request) {
        return ApiResult.<Page<UserResponse>>builder()
                .result(userService.getUsers(request))
                .build();
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieve a specific user's profile by their UUID. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<UserResponse> getUser(@PathVariable @Parameter(description = "User UUID") UUID userId) {
        return ApiResult.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    @Operation(summary = "Get my profile", description = "Retrieve the currently authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<UserResponse> getMyInfo() {
        return ApiResult.<UserResponse>builder().result(userService.getMyInfo()).build();
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update a user's profile information")
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<UserResponse> updateUser(
            @PathVariable @Parameter(description = "User UUID") UUID userId, @RequestBody UserUpdateRequest request) {
        return ApiResult.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Permanently delete a user account. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> deleteUser(@PathVariable @Parameter(description = "User UUID") UUID userId) {
        userService.deleteUser(userId);
        return ApiResult.<Void>builder().build();
    }
}
