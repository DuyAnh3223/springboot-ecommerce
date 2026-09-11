package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import spring.abtechzone.common.BaseIT;

class PaymentSchemaUpgradeIT extends BaseIT {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void additiveUpgradeIsRepeatableAndKeepsExistingRowsAndFinancialChecks() throws Exception {
        long before = jdbc.queryForObject("select count(*) from payment", Long.class);
        String sql = new String(
                new ClassPathResource("db/manual/commerce15-payment.sql")
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8);
        jdbc.execute(sql);
        jdbc.execute(sql);
        assertThat(jdbc.queryForObject("select count(*) from payment", Long.class))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject(
                        "select count(*) from pg_constraint where conrelid='payment'::regclass and contype='c' and pg_get_constraintdef(oid) like '%paid_at%'",
                        Integer.class))
                .isPositive();
        assertThat(jdbc.queryForObject(
                        "select count(*) from information_schema.columns where table_name='payment_checkout_key'",
                        Integer.class))
                .isEqualTo(4);
    }
}
