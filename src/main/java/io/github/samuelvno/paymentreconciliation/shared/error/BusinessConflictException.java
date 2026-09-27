package io.github.samuelvno.paymentreconciliation.shared.error;

public class BusinessConflictException extends RuntimeException {

    public BusinessConflictException(String message) {
        super(message);
    }
}
