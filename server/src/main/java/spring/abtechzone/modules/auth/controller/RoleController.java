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
import spring.abtechzone.modules.auth.dto.request.RoleRequest;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;
import spring.abtechzone.modules.auth.service.RoleService;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Roles", description = "Manage role definitions that group permissions (Admin only)")
public class RoleController {

    RoleService roleService;

    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role and assign permissions to it")
    @ApiResponse(responseCode = "200", description = "Role created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<RoleResponse> create(@RequestBody RoleRequest request) {
        return ApiResult.<RoleResponse>builder()
                .result(roleService.create(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieve the list of all defined roles")
    @ApiResponse(responseCode = "200", description = "Roles retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<List<RoleResponse>> getAll() {
        return ApiResult.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{role}")
    @Operation(summary = "Delete role", description = "Delete a role definition by name")
    @ApiResponse(responseCode = "200", description = "Role deleted")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> delete(@PathVariable String role) {
        roleService.delete(role);
        return ApiResult.<Void>builder().build();
    }
}
