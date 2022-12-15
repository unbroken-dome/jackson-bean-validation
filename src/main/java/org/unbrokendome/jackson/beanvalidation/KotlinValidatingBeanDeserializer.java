package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

import javax.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


class KotlinValidatingBeanDeserializer extends ValidatingBeanDeserializer {

    private static final Object INVALID_VALUE = new Object();

    private final boolean validateLateinitVars;


    KotlinValidatingBeanDeserializer(
            BeanDeserializerBase src, ValidatorFactory validatorFactory,
            BeanValidationFeatureSet features, @Nullable JsonValidated validationAnnotation
    ) {
        super(src, validatorFactory, features, validationAnnotation);

        validateLateinitVars = features.isEnabled(BeanValidationFeature.VALIDATE_KOTLIN_LATEINIT_VARS);
    }


    @Nullable
    @Override
    protected Set<ConstraintViolation<?>> _validateProperty(
            Object bean, String propName, @Nullable Set<ConstraintViolation<?>> violationsCollector
    ) {
        // This method will be called to validate any properties that were omitted in the JSON source.
        // For lateinit properties that will always mean a NotNull violation.
        if (validateLateinitVars) {
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (KotlinPropertyUtils.isKotlinLateinitVar(prop)) {
                ConstraintViolation<?> violation = _createNotNullViolation(bean, prop);

                if (violationsCollector == null) {
                    violationsCollector = new HashSet<>();
                }
                violationsCollector.add(violation);
                return violationsCollector;
            }
        }

        return super._validateProperty(bean, propName, violationsCollector);
    }


    @Override
    protected Set<? extends ConstraintViolation<?>> _validateValue(
            Class<?> beanType, SettableBeanProperty prop, @Nullable Object value
    ) {
        if (value == null && validateLateinitVars && KotlinPropertyUtils.isKotlinLateinitVar(prop)) {
            ConstraintViolation<?> violation = _createNotNullViolation(null, prop);
            return Collections.singleton(violation);

        } else {
            return super._validateValue(beanType, prop, value);
        }
    }


    @Override
    protected Object _deserializeProperty(
            JsonParser p, DeserializationContext ctxt, @Nullable Object bean, SettableBeanProperty prop
    ) throws IOException {

        Object value;
        Set<? extends ConstraintViolation<?>> violations;

        try {
            value = super._deserializeProperty(p, ctxt, bean, prop);
            violations = null;
        } catch (ConstraintViolationException ex) {
            value = INVALID_VALUE;
            violations = ex.getConstraintViolations();
        }

        if (value == null && !_hasAnyNotNullViolation(violations) && KotlinDetector.isKotlinReflectPresent()) {
            ConstraintViolation<?> notNullViolation = handleKotlinNull(bean, prop);
            if (notNullViolation != null) {
                if (violations == null) {
                    violations = Collections.singleton(notNullViolation);
                } else {
                    Set<ConstraintViolation<?>> newViolations = new HashSet<>(violations.size() + 1);
                    newViolations.addAll(violations);
                    newViolations.add(notNullViolation);
                    violations = newViolations;
                }
            }
        }

        if (violations != null) {
            throw new ConstraintViolationException(violations);
        }

        return value;
    }


    private boolean _hasAnyNotNullViolation(@Nullable Iterable<? extends ConstraintViolation<?>> violations) {
        if (violations != null) {
            for (ConstraintViolation<?> violation : violations) {
                if (violation.getConstraintDescriptor().getAnnotation() instanceof NotNull) {
                    return true;
                }
            }
        }
        return false;
    }


    @Nullable
    private ConstraintViolation<?> handleKotlinNull(@Nullable Object bean, SettableBeanProperty prop) {
        if (KotlinPropertyUtils.isNotNullProperty(prop)) {
            return _createNotNullViolation(bean, prop);
        }
        return null;
    }
}
