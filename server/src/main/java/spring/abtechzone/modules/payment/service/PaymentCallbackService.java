package spring.abtechzone.modules.payment.service;

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.gateway.*;

@Service
@RequiredArgsConstructor
public class PaymentCallbackService {
    private final GatewayRegistry gateways;
    private final OnlinePaymentService payments;

    public ResponseEntity<?> json(PaymentProvider provider, byte[] body) {
        try {
            if (body.length > 65536) return ResponseEntity.status(413).build();
            JsonNode data = GatewayCrypto.JSON.readTree(body);
            var result = gateways.require(provider).verifyCallback(data);
            var applied = payments.apply(provider, result);
            // payOS webhook registration sends a signed sample with no matching merchant attempt.
            if (applied == OnlinePaymentService.Applied.UNKNOWN && provider == PaymentProvider.PAYOS)
                return ResponseEntity.ok(Map.of("code", "00", "desc", "accepted"));
            if (applied == OnlinePaymentService.Applied.UNKNOWN)
                return ResponseEntity.notFound().build();
            if (applied == OnlinePaymentService.Applied.MISMATCH || applied == OnlinePaymentService.Applied.CONFLICT)
                return ResponseEntity.badRequest().build();
            return provider == PaymentProvider.MOMO
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.ok(Map.of("code", "00", "desc", "accepted"));
        } catch (GatewayException e) {
            return ResponseEntity.status(e.kind() == GatewayException.Kind.UNAVAILABLE ? 503 : 400)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JacksonException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<Map<String, String>> vnpay(MultiValueMap<String, String> params) {
        String code;
        try {
            if (params.size() > 40
                    || params.values().stream()
                            .anyMatch(v -> v.size() != 1 || v.getFirst().length() > 2048)) return ack("97");
            var payload = GatewayCrypto.JSON.valueToTree(params.toSingleValueMap());
            var result = gateways.require(PaymentProvider.VNPAY).verifyCallback(payload);
            code = switch (payments.apply(PaymentProvider.VNPAY, result)) {
                case RECORDED -> "00";
                case DUPLICATE -> "02";
                case UNKNOWN -> "01";
                case MISMATCH -> "04";
                case CONFLICT -> "02";
            };
        } catch (GatewayException e) {
            code = e.kind() == GatewayException.Kind.SIGNATURE ? "97" : "99";
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            code = "99";
        }
        return ack(code);
    }

    private ResponseEntity<Map<String, String>> ack(String code) {
        return ResponseEntity.ok(
                Map.of("RspCode", code, "Message", "00".equals(code) ? "Confirm Success" : "Payment not updated"));
    }
}
