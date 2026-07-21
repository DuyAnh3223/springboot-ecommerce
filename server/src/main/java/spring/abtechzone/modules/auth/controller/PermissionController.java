package spring.abtechzone.modules.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.auth.dto.request.PermissionRequest;
import spring.abtechzone.modules.auth.dto.response.PermissionResponse;
import spring.abtechzone.modules.auth.service.PermissionService;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Permissions", description = "Manage fine-grained permission definitions (Admin only)")
public class PermissionController {

    PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Create permission", description = "Create a new permission definition")
    @ApiResponse(responseCode = "200", description = "Permission created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<PermissionResponse> create(@RequestBody PermissionRequest request) {
        return ApiResult.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all permissions", description = "Retrieve the list of all defined permissions")
    @ApiResponse(responseCode = "200", description = "Permissions retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<List<PermissionResponse>> getAll() {
        return ApiResult.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    @DeleteMapping("/{permission}")
    @Operation(summary = "Delete permission", description = "Delete a permission definition by name")
    @ApiResponse(responseCode = "200", description = "Permission deleted")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> delete(@PathVariable String permission) {
        permissionService.delete(permission);
        return ApiResult.<Void>builder().build();
    }
}
