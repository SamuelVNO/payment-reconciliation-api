package io.github.samuelvno.paymentreconciliation.shared.messaging;

/** Evento que nunca vai ser processável (JSON inválido, header faltando): não adianta retry. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
