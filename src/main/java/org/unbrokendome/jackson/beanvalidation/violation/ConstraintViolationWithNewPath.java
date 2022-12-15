package org.unbrokendome.jackson.beanvalidation.violation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;


final class ConstraintViolationWithNewPath<T> extends AbstractConstraintViolation<T> {

    private final ConstraintViolation<T> delegate;
    private final Path propertyPath;


    ConstraintViolationWithNewPath(ConstraintViolation<T> delegate, Path propertyPath) {
        this.delegate = delegate;
        this.propertyPath = propertyPath;
    }


    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }


    @Override
    public String getMessage() {
        return delegate.getMessage();
    }


    @Override
    public String getMessageTemplate() {
        return delegate.getMessageTemplate();
    }


    @Override
    public T getRootBean() {
        return delegate.getRootBean();
    }


    @Override
    public Class<T> getRootBeanClass() {
        return delegate.getRootBeanClass();
    }


    @Override
    public Object getLeafBean() {
        return delegate.getLeafBean();
    }


    @Override
    public Object[] getExecutableParameters() {
        return delegate.getExecutableParameters();
    }


    @Override
    public Object getExecutableReturnValue() {
        return delegate.getExecutableReturnValue();
    }


    @Override
    public Object getInvalidValue() {
        return delegate.getInvalidValue();
    }


    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return delegate.getConstraintDescriptor();
    }
}
