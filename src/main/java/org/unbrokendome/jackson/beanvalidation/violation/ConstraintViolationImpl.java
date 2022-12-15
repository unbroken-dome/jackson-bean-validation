package org.unbrokendome.jackson.beanvalidation.violation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;


final class ConstraintViolationImpl<T> extends AbstractRootedConstraintViolation<T> {

    private final String message;
    private final Object leafBean;
    private final Path propertyPath;
    private final Object invalidValue;
    private final ConstraintDescriptor<?> constraintDescriptor;


    ConstraintViolationImpl(String message,
                            @Nullable T rootBean,
                            Class<T> rootBeanClass,
                            @Nullable Object leafBean,
                            Path propertyPath,
                            @Nullable Object invalidValue,
                            ConstraintDescriptor<?> constraintDescriptor) {
        super(rootBean, rootBeanClass);
        this.message = message;
        this.leafBean = leafBean;
        this.propertyPath = propertyPath;
        this.invalidValue = invalidValue;
        this.constraintDescriptor = constraintDescriptor;
    }


    @Override
    @Nonnull
    public String getMessage() {
        return message;
    }


    @Override
    @Nonnull
    public String getMessageTemplate() {
        return constraintDescriptor.getMessageTemplate();
    }


    @Override
    @Nullable
    public Object getLeafBean() {
        return leafBean;
    }


    @Override
    @Nullable
    public Object[] getExecutableParameters() {
        return null;
    }


    @Override
    @Nullable
    public Object getExecutableReturnValue() {
        return null;
    }


    @Override
    @Nonnull
    public Path getPropertyPath() {
        return propertyPath;
    }


    @Override
    @Nullable
    public Object getInvalidValue() {
        return invalidValue;
    }


    @Override
    @Nonnull
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }
}
