package spring.abtechzone.modules.order.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a list of SKU IDs contains no duplicates. Applied to the
 * reviewed checkout items list; SKU IDs must be unique (SPEC-COMMERCE-04,
 * R-C04-07 / edge case: duplicate SKU rejected with HTTP 400).
 */
@Documented
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = UniqueSkuIdsValidator.class)
public @interface UniqueSkuIds {

    String message() default "reviewedCheckout.items must have unique SKU IDs";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
