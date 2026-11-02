package example.logistics;

public final class InfraiException extends Exception {
    private final String code;
    private final int status;

    public InfraiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }
}
