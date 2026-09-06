package spring.abtechzone.modules.payment.gateway;

public class GatewayException extends RuntimeException {
    public enum Kind {
        SIGNATURE,
        DATA,
        UNAVAILABLE
    }

    private final Kind kind;

    public GatewayException(Kind kind) {
        super("Payment gateway " + kind.name());
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
