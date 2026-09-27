package io.github.samuelvno.paymentreconciliation.shared.error;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
