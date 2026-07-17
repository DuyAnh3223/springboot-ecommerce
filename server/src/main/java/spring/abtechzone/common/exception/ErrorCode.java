package spring.abtechzone.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9998, "Invalid Message Key", HttpStatus.BAD_REQUEST),
    USER_EXISTS(1001, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 3 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Unauthorized", HttpStatus.FORBIDDEN),
    PRODUCT_NOT_FOUND(1008, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_SKU_EXISTS(1009, "Product SKU already exists", HttpStatus.BAD_REQUEST),
    SKU_NOT_FOUND(1010, "SKU not found", HttpStatus.NOT_FOUND),
    PRODUCT_ATTRIBUTES_INVALID(1011, "Product attributes do not match existing SKUs", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_INVALID(1012, "Product name is required", HttpStatus.BAD_REQUEST),
    PRODUCT_SKU_INVALID(1013, "Product SKU is required", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID(1014, "Product price must be zero or greater", HttpStatus.BAD_REQUEST),
    PRODUCT_STOCK_INVALID(1015, "Product stock must be zero or greater", HttpStatus.BAD_REQUEST),
    PRODUCT_PAGE_INVALID(1016, "Product page must be one or greater", HttpStatus.BAD_REQUEST),
    PRODUCT_SIZE_INVALID(1017, "Product size must be one or greater", HttpStatus.BAD_REQUEST),
    PRODUCT_SLUG_EXISTS(1018, "Product slug already exists", HttpStatus.BAD_REQUEST),
    PRODUCT_SLUG_INVALID(1019, "Product slug is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_DATE_INVALID(1020, "Voucher date is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_VALUE_INVALID(1021, "Voucher value is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_SCOPE_INVALID(1021, "Voucher scope is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND(1022, "Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_EXISTED(1023, "Voucher already exists", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED(1024, "Voucher expired", HttpStatus.BAD_REQUEST),
    VOUCHER_ARE_OUT(1025, "Voucher are out", HttpStatus.BAD_REQUEST),
    VOUCHER_MIN_ORDER_VALUE_INVALID(1026, "Voucher minimum order value is invalid", HttpStatus.BAD_REQUEST),
    CART_EXISTS(1027, "Cart already exists", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(1028, "Cart item not found", HttpStatus.NOT_FOUND),
    CART_ITEM_QUANTITY_INVALID(1029, "Cart item quantity must be at least 1", HttpStatus.BAD_REQUEST),
    CART_NOT_FOUND(1030, "Cart not found", HttpStatus.NOT_FOUND),
    CART_IS_EMPTY(1031, "Cart is empty", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(1032, "Insufficient stock for product", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_AVAILABLE(1033, "Product is not available for sale", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1034, "Order not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(1035, "Address not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_BELONG_TO_USER(1036, "Address does not belong to user", HttpStatus.FORBIDDEN),
    ADDRESS_REQUIRED(1037, "Address is required", HttpStatus.BAD_REQUEST),
    VOUCHER_PER_USER_LIMIT_REACHED(1038, "Voucher per-user usage limit reached", HttpStatus.BAD_REQUEST),
    CATEGORY_EXISTED(1039, "Category already exists", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(1040, "Category not found", HttpStatus.NOT_FOUND),
    BRAND_EXISTED(1041, "Brand already exists", HttpStatus.BAD_REQUEST),
    BRAND_NOT_FOUND(1042, "Brand not found", HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND(1043, "Reservation not found", HttpStatus.NOT_FOUND),
    SYSTEM_BUSY(1044, "System busy, try again later", HttpStatus.BAD_REQUEST),
    SYSTEM_ERROR(1045, "System error, try again later", HttpStatus.BAD_REQUEST),
    ATTRIBUTE_EXISTS(1046, "Attribute already exists", HttpStatus.BAD_REQUEST),
    ATTRIBUTE_NOT_FOUND(1047, "Attribute not found", HttpStatus.NOT_FOUND),
    CATEGORY_ATTRIBUTE_ALREADY_ASSIGNED(1048, "Attribute is already assigned to this category", HttpStatus.CONFLICT),
    CATEGORY_ATTRIBUTE_NOT_FOUND(1049, "Category attribute assignment not found", HttpStatus.NOT_FOUND),
    PRODUCT_ATTRIBUTES_REQUIRED(1050, "Product attributes required", HttpStatus.BAD_REQUEST),
    PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING(1051, "Product sku variant attributes missing", HttpStatus.BAD_REQUEST),
    PRODUCT_SKU_ATTRIBUTES_DUPLICATED(1052, "Product sku variant attributes duplicated", HttpStatus.BAD_REQUEST),
    CATEGORY_REQUIRED(1053, "Category required", HttpStatus.BAD_REQUEST),
    ATTRIBUTE_ENUM_VALUES_MISSING(1054, "Attribute values missing", HttpStatus.BAD_REQUEST),
    VARIANT_CANNOT_BE_MULTI_VALUE(1055, "Variant-defining attribute cannot be multi-value", HttpStatus.BAD_REQUEST),
    ATTRIBUTE_VALUE_INVALID(1056, "Attribute value is invalid for the defined type", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_REQUIRED(1057, "Product must belong to a category", HttpStatus.BAD_REQUEST),
    VARIANT_ATTRIBUTE_MUST_BE_ENUM(1058, "Variant-defining attribute must have ENUM data type", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
