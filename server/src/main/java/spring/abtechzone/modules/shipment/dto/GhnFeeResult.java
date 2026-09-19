package spring.abtechzone.modules.shipment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GhnFeeResult(BigDecimal totalFee, OffsetDateTime quotedAt) {}
