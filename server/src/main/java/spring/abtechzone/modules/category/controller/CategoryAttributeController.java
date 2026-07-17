package spring.abtechzone.modules.category.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.category.dto.request.AssignAttributeRequest;
import spring.abtechzone.modules.category.dto.response.CategoryAttributeResponse;
import spring.abtechzone.modules.category.service.CategoryAttributeService;

@RestController
@RequestMapping("/categories/{categoryId}/attributes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryAttributeController {

    CategoryAttributeService categoryAttributeService;

    @PostMapping
    ApiResponse<List<CategoryAttributeResponse>> assignAttributes(
            @PathVariable("categoryId") Long categoryId, @Valid @RequestBody List<AssignAttributeRequest> requests) {
        return ApiResponse.<List<CategoryAttributeResponse>>builder()
                .result(categoryAttributeService.assignAttributes(categoryId, requests))
                .build();
    }

    @GetMapping
    ApiResponse<List<CategoryAttributeResponse>> getAttributesByCategory(@PathVariable("categoryId") Long categoryId) {
        return ApiResponse.<List<CategoryAttributeResponse>>builder()
                .result(categoryAttributeService.getAttributesByCategory(categoryId))
                .build();
    }

    @PatchMapping("/{id}")
    ApiResponse<CategoryAttributeResponse> updateCategoryAttribute(
            @PathVariable("categoryId") Long categoryId,
            @PathVariable("id") Long id,
            @Valid @RequestBody AssignAttributeRequest request) {
        return ApiResponse.<CategoryAttributeResponse>builder()
                .result(categoryAttributeService.updateCategoryAttribute(id, request))
                .build();
    }

    @DeleteMapping("/{attributeId}")
    ApiResponse<Void> removeCategoryAttribute(
            @PathVariable("categoryId") Long categoryId, @PathVariable("attributeId") Long attributeId) {
        categoryAttributeService.removeCategoryAttribute(categoryId, attributeId);
        return ApiResponse.<Void>builder().build();
    }
}
