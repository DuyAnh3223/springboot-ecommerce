package spring.abtechzone.modules.payment.dto;

import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.constant.PaymentStatus;

public record PaymentSummary(PaymentMethod method, PaymentStatus status) {}
