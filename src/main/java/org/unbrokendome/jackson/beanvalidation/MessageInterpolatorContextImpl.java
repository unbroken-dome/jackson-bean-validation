package org.unbrokendome.jackson.beanvalidation;

import javax.validation.MessageInterpolator;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;


class MessageInterpolatorContextImpl implements MessageInterpolator.Context {

    private final ConstraintDescriptor<?> constraintDescriptor;
    private final Object validatedValue;


    MessageInterpolatorContextImpl(ConstraintDescriptor<?> constraintDescriptor, Object validatedValue) {
        this.constraintDescriptor = constraintDescriptor;
        this.validatedValue = validatedValue;
    }


    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }


    @Override
    public Object getValidatedValue() {
        return validatedValue;
    }


    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isAssignableFrom(MessageInterpolator.Context.class)) {
            return type.cast(this);
        } else {
            throw new ValidationException("Type " + type + " is not supported for unwrapping.");
        }
    }
}
