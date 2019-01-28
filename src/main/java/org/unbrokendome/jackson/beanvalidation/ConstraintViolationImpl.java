package org.unbrokendome.jackson.beanvalidation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;


class ConstraintViolationImpl<T> implements ConstraintViolation<T> {

    private final String message;
    private final T rootBean;
    private final Class<T> rootBeanClass;
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
        this.message = message;
        this.rootBean = rootBean;
        this.rootBeanClass = rootBeanClass;
        this.leafBean = leafBean;
        this.propertyPath = propertyPath;
        this.invalidValue = invalidValue;
        this.constraintDescriptor = constraintDescriptor;
    }


    ConstraintViolationImpl(@Nullable T rootBean,
                            Class<T> rootBeanClass,
                            @Nullable Object leafBean,
                            Path propertyPath,
                            @Nullable Object invalidValue,
                            ConstraintDescriptor<?> descriptor,
                            MessageInterpolator messageInterpolator) {
        this(
                messageInterpolator.interpolate(
                        descriptor.getMessageTemplate(),
                        new MessageInterpolatorContextImpl(descriptor, invalidValue)),
                rootBean, rootBeanClass, leafBean, propertyPath, invalidValue, descriptor
        );
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
    public T getRootBean() {
        return rootBean;
    }


    @Override
    @Nonnull
    public Class<T> getRootBeanClass() {
        return rootBeanClass;
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


    @Override
    public <U> U unwrap(Class<U> type) {
        if (type.isAssignableFrom(ConstraintViolation.class)) {
            return type.cast(this);
        } else {
            throw new ValidationException("Type " + type + " is not supported for unwrapping.");
        }
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("ConstraintViolation[");

        builder.append("propertyPath=").append(propertyPath);
        if (message != null) {
            builder.append(", message=\"").append(message).append("\"");
        }
        if (rootBean != null) {
            builder.append(", rootBean=").append(rootBean);
        }
        if (rootBeanClass != null) {
            builder.append(", rootBeanClass=").append(rootBeanClass.getName());
        }
        builder.append(", invalidValue=\"").append(invalidValue).append("\"");

        return builder
                .append("]")
                .toString();
    }
}
