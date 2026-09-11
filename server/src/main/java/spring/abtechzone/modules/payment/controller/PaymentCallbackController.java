package spring.abtechzone.modules.payment.controller;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.service.PaymentCallbackService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/callbacks")
public class PaymentCallbackController {
    private final PaymentCallbackService callbacks;

    @PostMapping(value = "/momo", consumes = "application/json")
    public ResponseEntity<?> momo(HttpServletRequest request) throws IOException {
        return callbacks.json(PaymentProvider.MOMO, request.getInputStream().readNBytes(65537));
    }

    @PostMapping(value = "/payos", consumes = "application/json")
    public ResponseEntity<?> payos(HttpServletRequest request) throws IOException {
        return callbacks.json(PaymentProvider.PAYOS, request.getInputStream().readNBytes(65537));
    }

    @GetMapping("/vnpay")
    public ResponseEntity<Map<String, String>> vnpay(@RequestParam MultiValueMap<String, String> params) {
        return callbacks.vnpay(params);
    }
}
