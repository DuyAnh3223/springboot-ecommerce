package spring.abtechzone.modules.inventory.mapper;

import java.time.OffsetDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.inventory.constant.StockMovementReason;
import spring.abtechzone.modules.inventory.dto.response.StockAdjustmentResponse;
import spring.abtechzone.modules.inventory.dto.response.StockMovementResponse;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository.StockMovementHistoryProjection;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.user.entity.User;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "changeQty", source = "changeQty")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "referenceId", source = "referenceId")
    @Mapping(target = "createdBy", source = "actor")
    @Mapping(target = "createdAt", source = "createdAt")
    StockMovement toEntity(
            ProductSku sku,
            int changeQty,
            StockMovementReason reason,
            String referenceId,
            User actor,
            OffsetDateTime createdAt);

    @Mapping(source = "id", target = "movementId")
    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.sku", target = "skuCode")
    @Mapping(source = "createdBy.username", target = "createdBy")
    StockMovementResponse toResponse(StockMovement movement);

    StockMovementResponse toResponse(StockMovementHistoryProjection movement);

    @Mapping(source = "skuId", target = "skuId")
    @Mapping(source = "onHand", target = "onHand")
    @Mapping(source = "movement", target = "movement")
    StockAdjustmentResponse toAdjustmentResponse(Long skuId, Integer onHand, StockMovement movement);

    default StockMovementReason mapReason(String reason) {
        return reason == null ? null : StockMovementReason.valueOf(reason);
    }
}
