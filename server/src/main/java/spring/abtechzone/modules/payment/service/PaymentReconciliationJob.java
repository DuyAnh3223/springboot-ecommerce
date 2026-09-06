package spring.abtechzone.modules.payment.service;

import java.time.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.modules.payment.repository.PaymentRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {
    private final PaymentRepository payments;
    private final OnlinePaymentService service;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.payment.online.poll-delay-ms:15000}")
    public void run() {
        var now = OffsetDateTime.now(clock);
        for (Long id : payments.findExpired(now, PageRequest.of(0, 20))) {
            try {
                service.expire(id);
            } catch (RuntimeException e) {
                log.warn("Payment expiry retry needed: paymentId={}", id);
            }
        }
        for (Long id : payments.findDue(now, PageRequest.of(0, 10))) {
            try {
                service.reconcile(id);
            } catch (RuntimeException e) {
                log.warn("Payment query retry needed: paymentId={}", id);
            }
        }
    }
}
