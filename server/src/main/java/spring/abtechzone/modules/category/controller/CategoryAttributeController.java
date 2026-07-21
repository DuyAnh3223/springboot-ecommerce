package spring.abtechzone.modules.category.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.category.dto.request.AssignAttributeRequest;
import spring.abtechzone.modules.category.dto.response.CategoryAttributeResponse;
import spring.abtechzone.modules.category.service.CategoryAttributeService;

@RestController
@RequestMapping("/categories/{categoryId}/attributes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Category Attributes",
        description =
                "Manage the attribute schema assigned to a category. Determines which attributes products in that category must provide")
public class CategoryAttributeController {

    CategoryAttributeService categoryAttributeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Assign attributes to category",
            description = "Assign one or more attribute definitions to a category. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Attributes assigned")
    @ApiResponse(responseCode = "400", description = "Attribute already assigned or validation error")
    @ApiResponse(responseCode = "404", description = "Category or attribute not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<List<CategoryAttributeResponse>> assignAttributes(
            @PathVariable @Parameter(description = "Category ID") Long categoryId,
            @Valid @RequestBody List<AssignAttributeRequest> requests) {
        return ApiResult.<List<CategoryAttributeResponse>>builder()
                .result(categoryAttributeService.assignAttributes(categoryId, requests))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get attributes by category",
            description = "Retrieve all attribute assignments for a category")
    @ApiResponse(responseCode = "200", description = "Category attributes retrieved")
    @ApiResponse(responseCode = "404", description = "Category not found")
    ApiResult<List<CategoryAttributeResponse>> getAttributesByCategory(
            @PathVariable @Parameter(description = "Category ID") Long categoryId) {
        return ApiResult.<List<CategoryAttributeResponse>>builder()
                .result(categoryAttributeService.getAttributesByCategory(categoryId))
                .build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update category attribute assignment",
            description =
                    "Update configuration (e.g. isVariant, isRequired) of an attribute assignment. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Assignment updated")
    @ApiResponse(responseCode = "404", description = "Assignment not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<CategoryAttributeResponse> updateCategoryAttribute(
            @PathVariable @Parameter(description = "Category ID") Long categoryId,
            @PathVariable @Parameter(description = "Category-Attribute assignment ID") Long id,
            @Valid @RequestBody AssignAttributeRequest request) {
        return ApiResult.<CategoryAttributeResponse>builder()
                .result(categoryAttributeService.updateCategoryAttribute(id, request))
                .build();
    }

    @DeleteMapping("/{attributeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove attribute from category",
            description = "Remove an attribute assignment from a category. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Attribute removed from category")
    @ApiResponse(responseCode = "404", description = "Assignment not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> removeCategoryAttribute(
            @PathVariable @Parameter(description = "Category ID") Long categoryId,
            @PathVariable @Parameter(description = "Attribute ID") Long attributeId) {
        categoryAttributeService.removeCategoryAttribute(categoryId, attributeId);
        return ApiResult.<Void>builder().build();
    }
}
