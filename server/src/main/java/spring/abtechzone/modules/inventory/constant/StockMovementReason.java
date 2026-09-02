package spring.abtechzone.modules.inventory.constant;

public enum StockMovementReason {
    OPENING_BALANCE,
    PURCHASE_IN,
    DAMAGE_OUT,
    MANUAL_ADJUSTMENT_IN,
    MANUAL_ADJUSTMENT_OUT,
    SALE_OUT,
    ORDER_CANCEL_RETURN,

    // Legacy values retained so existing movement rows remain readable.
    RETURN_IN,
    ADJUSTMENT,
    WARRANTY_REPLACE
}
