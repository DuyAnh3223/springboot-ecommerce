package spring.abtechzone.modules.category.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.category.dto.request.AttributeRequest;
import spring.abtechzone.modules.category.dto.request.AttributeSearchRequest;
import spring.abtechzone.modules.category.dto.response.AttributeResponse;
import spring.abtechzone.modules.category.service.AttributeService;

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

    @GetMapping
    ApiResponse<Page<AttributeResponse>> getGlobalAttributes(@Valid @ModelAttribute AttributeSearchRequest request) {
        return ApiResponse.<Page<AttributeResponse>>builder()
                .result(attributeService.getGlobalAttributes(request))
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
