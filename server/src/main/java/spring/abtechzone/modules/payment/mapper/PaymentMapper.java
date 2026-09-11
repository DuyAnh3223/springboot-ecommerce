package spring.abtechzone.modules.payment.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.modules.payment.dto.response.PaymentAttemptResponse;
import spring.abtechzone.modules.payment.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentAttemptResponse toResponse(Payment payment);
}
