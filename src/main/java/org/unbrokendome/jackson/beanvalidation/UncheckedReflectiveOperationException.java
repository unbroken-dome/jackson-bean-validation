package org.unbrokendome.jackson.beanvalidation;


public final class UncheckedReflectiveOperationException extends RuntimeException {

    public UncheckedReflectiveOperationException(String message, ReflectiveOperationException cause) {
        super(message, cause);
    }
}
