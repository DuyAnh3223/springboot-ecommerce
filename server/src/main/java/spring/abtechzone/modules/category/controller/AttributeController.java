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
import spring.abtechzone.modules.category.dto.request.AttributeRequest;
import spring.abtechzone.modules.category.dto.request.AttributeSearchRequest;
import spring.abtechzone.modules.category.dto.response.AttributeResponse;
import spring.abtechzone.modules.category.service.AttributeService;

@RestController
@RequestMapping("/attributes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Attributes",
        description =
                "Global product attribute definitions (e.g. Color, Size, RAM). Used to configure category-level schemas and drive SKU variant generation")
public class AttributeController {

    AttributeService attributeService;

    @PostMapping
    @Operation(summary = "Create attribute", description = "Define a new global attribute. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Attribute created")
    @ApiResponse(responseCode = "400", description = "Attribute already exists or invalid request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<AttributeResponse> createAttribute(@Valid @RequestBody AttributeRequest attributeRequest) {
        return ApiResult.<AttributeResponse>builder()
                .result(attributeService.createAttributeDefinition(attributeRequest))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get attributes (paginated)",
            description = "Retrieve a paginated list of all global attribute definitions")
    @ApiResponse(responseCode = "200", description = "Attributes retrieved")
    ApiResult<Page<AttributeResponse>> getGlobalAttributes(@Valid @ModelAttribute AttributeSearchRequest request) {
        return ApiResult.<Page<AttributeResponse>>builder()
                .result(attributeService.getGlobalAttributes(request))
                .build();
    }

    @GetMapping("/{attributeId}")
    @Operation(summary = "Get attribute by ID", description = "Retrieve a single attribute definition by its ID")
    @ApiResponse(responseCode = "200", description = "Attribute found")
    @ApiResponse(responseCode = "404", description = "Attribute not found")
    ApiResult<AttributeResponse> getAttribute(@PathVariable @Parameter(description = "Attribute ID") Long attributeId) {
        return ApiResult.<AttributeResponse>builder()
                .result(attributeService.getAttribute(attributeId))
                .build();
    }

    @PatchMapping("/{attributeId}")
    @Operation(
            summary = "Update attribute",
            description = "Update an attribute's name, type, or enum values. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Attribute updated")
    @ApiResponse(responseCode = "404", description = "Attribute not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<AttributeResponse> updateAttribute(
            @PathVariable @Parameter(description = "Attribute ID") Long attributeId,
            @Valid @RequestBody AttributeRequest attributeRequest) {
        return ApiResult.<AttributeResponse>builder()
                .result(attributeService.updateAttribute(attributeId, attributeRequest))
                .build();
    }

    @DeleteMapping("/{attributeId}")
    @Operation(summary = "Delete attribute", description = "Delete a global attribute definition. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Attribute deleted")
    @ApiResponse(responseCode = "404", description = "Attribute not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> deleteAttribute(@PathVariable @Parameter(description = "Attribute ID") Long attributeId) {
        attributeService.deleteAttribute(attributeId);
        return ApiResult.<Void>builder().build();
    }
}
