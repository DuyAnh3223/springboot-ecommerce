package spring.abtechzone.modules.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.auth.dto.request.RoleRequest;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;
import spring.abtechzone.modules.auth.service.RoleService;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    @PostMapping
    ApiResponse<RoleResponse> create(@RequestBody RoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{role}")
    ApiResponse<Void> delete(@PathVariable("role") String role) {
        roleService.delete(role);
        return ApiResponse.<Void>builder().build();
    }
}
