package org.unbrokendome.jackson.beanvalidation;

import jakarta.validation.ConstraintTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Payload;
import jakarta.validation.ValidationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ValidateUnwrappedValue;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


abstract class AbstractConstraintDescriptor<T extends Annotation> implements ConstraintDescriptor<T> {

    private final T annotation;


    AbstractConstraintDescriptor(T annotation) {
        this.annotation = annotation;
    }


    @Override
    public final T getAnnotation() {
        return annotation;
    }


    @Override
    public final String getMessageTemplate() {
        return getMessageFromAnnotation(annotation);
    }


    protected abstract String getMessageFromAnnotation(T annotation);


    @Override
    public final Set<Class<?>> getGroups() {
        return new LinkedHashSet<>(Arrays.asList(getGroupsFromAnnotation(annotation)));
    }


    protected abstract Class<?>[] getGroupsFromAnnotation(T annotation);


    @Override
    public final Set<Class<? extends Payload>> getPayload() {
        return new LinkedHashSet<>(Arrays.asList(getPayloadFromAnnotation(annotation)));
    }


    protected abstract Class<? extends Payload>[] getPayloadFromAnnotation(T annotation);


    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return null;
    }


    @Override
    public final List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
        return Collections.emptyList();
    }


    @Override
    public final Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }


    @Override
    public final Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return Collections.emptySet();
    }


    @Override
    public final boolean isReportAsSingleViolation() {
        return false;
    }


    @Override
    public final ValidateUnwrappedValue getValueUnwrapping() {
        return ValidateUnwrappedValue.DEFAULT;
    }


    @Override
    public final <U> U unwrap(Class<U> type) {
        if (type.isAssignableFrom(ConstraintDescriptor.class)) {
            return type.cast(this);
        } else {
            throw new ValidationException("Type " + type + " is not supported for unwrapping.");
        }
    }
}
