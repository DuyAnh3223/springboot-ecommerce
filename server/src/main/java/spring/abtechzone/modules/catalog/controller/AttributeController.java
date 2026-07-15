package spring.abtechzone.modules.catalog.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.catalog.dto.request.AttributeRequest;
import spring.abtechzone.modules.catalog.dto.request.AttributeSearchRequest;
import spring.abtechzone.modules.catalog.dto.response.AttributeResponse;
import spring.abtechzone.modules.catalog.service.AttributeService;

@RestController
@RequestMapping("/attributes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttributeController {

    AttributeService attributeService;

    @PostMapping
    ApiResponse<AttributeResponse> createAttribute(@Valid @RequestBody AttributeRequest attributeRequest) {
        return ApiResponse.<AttributeResponse>builder()
                .result(attributeService.createAttributeDefinition(attributeRequest))
                .build();
    }

    @GetMapping("/category/{categoryId}")
    ApiResponse<Page<AttributeResponse>> getAttributesByCategoryId(
            @PathVariable("categoryId") Long categoryId, @Valid @ModelAttribute AttributeSearchRequest request) {
        return ApiResponse.<Page<AttributeResponse>>builder()
                .result(attributeService.getAttributesByCategoryId(categoryId, request))
                .build();
    }

    @GetMapping("/{attributeId}")
    ApiResponse<AttributeResponse> getAttribute(@PathVariable("attributeId") Long attributeId) {
        return ApiResponse.<AttributeResponse>builder()
                .result(attributeService.getAttribute(attributeId))
                .build();
    }

    @PatchMapping("/{attributeId}")
    ApiResponse<AttributeResponse> updateAttribute(
            @PathVariable("attributeId") Long attributeId, @Valid @RequestBody AttributeRequest attributeRequest) {
        return ApiResponse.<AttributeResponse>builder()
                .result(attributeService.updateAttribute(attributeId, attributeRequest))
                .build();
    }

    @DeleteMapping("/{attributeId}")
    ApiResponse<Void> deleteAttribute(@PathVariable("attributeId") Long attributeId) {
        attributeService.deleteAttribute(attributeId);
        return ApiResponse.<Void>builder().build();
    }
}
