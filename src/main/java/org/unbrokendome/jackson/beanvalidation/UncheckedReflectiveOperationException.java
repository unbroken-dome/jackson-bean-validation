package org.unbrokendome.jackson.beanvalidation;


final class UncheckedReflectiveOperationException extends RuntimeException {

    UncheckedReflectiveOperationException(String message, ReflectiveOperationException cause) {
        super(message, cause);
    }
}
