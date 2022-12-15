package org.unbrokendome.jackson.beanvalidation.violation;

import javax.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;


public final class ConstraintViolationUtils {

    private ConstraintViolationUtils() {
    }


    public static <T> ConstraintViolation<T> create(
            @Nullable T rootBean, Class<T> rootBeanClass,
            @Nullable Object leafBean, Path propertyPath, @Nullable Object invalidValue,
            ConstraintDescriptor<?> constraintDescriptor, String message) {

        return new ConstraintViolationImpl<>(message, rootBean, rootBeanClass, leafBean, propertyPath,
                invalidValue, constraintDescriptor);
    }


    public static <T> ConstraintViolation<T> create(
            @Nullable T rootBean, Class<T> rootBeanClass,
            @Nullable Object leafBean, Path propertyPath, @Nullable Object invalidValue,
            ConstraintDescriptor<?> constraintDescriptor, MessageInterpolator messageInterpolator) {

        MessageInterpolator.Context interpolatorContext =
                new MessageInterpolatorContextImpl(constraintDescriptor, invalidValue);

        String message = messageInterpolator.interpolate(
                constraintDescriptor.getMessageTemplate(),
                interpolatorContext);

        return create(rootBean, rootBeanClass, leafBean, propertyPath, invalidValue, constraintDescriptor, message);
    }


    public static <T> ConstraintViolation<T> withBasePath(
            ConstraintViolation<?> violation,
            @Nullable T newRootBean,
            Class<T> newRootBeanClass,
            Path basePath) {

        return new CompositeConstraintViolation<>(newRootBean, newRootBeanClass, basePath, violation);
    }


    public static <T> ConstraintViolation<T> withNewPath(
            ConstraintViolation<T> violation,
            Path path) {

        return new ConstraintViolationWithNewPath<>(violation, path);
    }
}
