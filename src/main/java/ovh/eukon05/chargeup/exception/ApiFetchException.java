package ovh.eukon05.chargeup.exception;

public class ApiFetchException extends RuntimeException {
    private static final String IO_MESSAGE = "An exception occurred while trying to fetch from the API";

    public ApiFetchException() {
        super(String.format(IO_MESSAGE));
    }
}
