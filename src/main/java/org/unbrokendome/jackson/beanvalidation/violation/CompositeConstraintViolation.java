package org.unbrokendome.jackson.beanvalidation.violation;

import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;


final class CompositeConstraintViolation<T> extends AbstractRootedConstraintViolation<T> {

    @Nonnull
    private final ConstraintViolation<?> other;

    private final Path propertyPath;


    public CompositeConstraintViolation(@Nullable T rootBean, Class<T> rootBeanClass,
                                        Path basePath,
                                        ConstraintViolation<?> other) {
        super(rootBean, rootBeanClass);
        this.other = other;

        this.propertyPath = PathBuilder.create()
                .appendPath(basePath)
                .appendPath(other.getPropertyPath())
                .build();
    }


    @Override
    public String getMessage() {
        return other.getMessage();
    }


    @Override
    public String getMessageTemplate() {
        return other.getMessageTemplate();
    }


    @Override
    public Object getLeafBean() {
        return other.getLeafBean();
    }


    @Override
    public Object[] getExecutableParameters() {
        return other.getExecutableParameters();
    }


    @Override
    public Object getExecutableReturnValue() {
        return other.getExecutableReturnValue();
    }


    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }


    @Override
    public Object getInvalidValue() {
        return other.getInvalidValue();
    }


    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return other.getConstraintDescriptor();
    }
}
