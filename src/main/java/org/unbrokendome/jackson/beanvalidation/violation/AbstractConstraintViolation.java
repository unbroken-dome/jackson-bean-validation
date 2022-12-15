package org.unbrokendome.jackson.beanvalidation.violation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;


abstract class AbstractConstraintViolation<T> implements ConstraintViolation<T> {

    @Override
    public final <U> U unwrap(Class<U> type) {
        if (type.isAssignableFrom(ConstraintViolation.class)) {
            return type.cast(this);
        } else {
            throw new ValidationException("Type " + type + " is not supported for unwrapping.");
        }
    }


    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder()
                .append("ConstraintViolation[");

        builder.append("propertyPath=").append(getPropertyPath());

        String message = getMessage();
        if (message != null) {
            builder.append(", message=\"").append(message).append("\"");
        }

        Object rootBean = getRootBean();
        if (rootBean != null) {
            builder.append(", rootBean=").append(rootBean);
        }

        builder.append(", rootBeanClass=").append(getRootBeanClass().getName());

        builder.append(", invalidValue=\"").append(getInvalidValue()).append("\"");

        return builder
                .append("]")
                .toString();
    }
}
