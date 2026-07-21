package spring.abtechzone.modules.category.controller;

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
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.category.dto.request.CategoryRequest;
import spring.abtechzone.modules.category.dto.request.CategorySearchRequest;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;
import spring.abtechzone.modules.category.service.CategoryService;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Categories", description = "Product category management")
public class CategoryController {

    CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create category", description = "Create a new product category. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Category name already exists or validation error")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ApiResult.<CategoryResponse>builder()
                .result(categoryService.create(categoryRequest))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get categories (paginated)",
            description = "Retrieve a paginated, searchable list of categories")
    @ApiResponse(responseCode = "200", description = "Categories retrieved")
    ApiResult<Page<CategoryResponse>> getCategories(@Valid @ModelAttribute CategorySearchRequest request) {
        return ApiResult.<Page<CategoryResponse>>builder()
                .result(categoryService.getCategories(request))
                .build();
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID", description = "Retrieve a single category by its ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")
    ApiResult<CategoryResponse> getCategory(@PathVariable @Parameter(description = "Category ID") Long categoryId) {
        return ApiResult.<CategoryResponse>builder()
                .result(categoryService.getCategory(categoryId))
                .build();
    }

    @PatchMapping("/{categoryId}")
    @Operation(
            summary = "Update category",
            description = "Update a category's name or description. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<CategoryResponse> updateCategory(
            @PathVariable @Parameter(description = "Category ID") Long categoryId,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        return ApiResult.<CategoryResponse>builder()
                .result(categoryService.updateCategory(categoryId, categoryRequest))
                .build();
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete category", description = "Delete a category by ID. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Category deleted")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> deleteCategory(@PathVariable @Parameter(description = "Category ID") Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResult.<Void>builder().build();
    }
}
