package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.util.ClassUtil;
import org.unbrokendome.jackson.beanvalidation.violation.ConstraintViolationUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Extension of {@link PropertyValueBuffer} that also stores constraint violations for each property.
 */
class ValidationAwarePropertyValueBuffer extends PropertyValueBuffer {

    private final JavaType beanType;
    private final BeanValidationFeatureSet features;
    private final MessageInterpolator messageInterpolator;
    private Map<Integer, Set<ConstraintViolation<?>>> parameterViolations;
    private final JsonValidated validationAnnotation;


    ValidationAwarePropertyValueBuffer(
            JsonParser p, DeserializationContext ctxt, int paramCount, ObjectIdReader oir,
            JavaType beanType, BeanValidationFeatureSet features, MessageInterpolator messageInterpolator,
            JsonValidated validationAnnotation
    ) {
        super(p, ctxt, paramCount, oir);
        this.beanType = beanType;
        this.features = features;
        this.messageInterpolator = messageInterpolator;
        this.validationAnnotation = validationAnnotation;
    }


    void assignViolations(SettableBeanProperty prop, Set<ConstraintViolation<?>> violations) {

        if (parameterViolations == null) {
            parameterViolations = new HashMap<>(this._creatorParameters.length);
        }

        int index = prop.getCreatorIndex();
        parameterViolations.put(index, violations);
    }


    @Override
    protected Object _findMissing(SettableBeanProperty prop) throws JsonMappingException {

        assert prop instanceof CreatorProperty;

        // First: do we have injectable value?
        Object injectableValueId = prop.getInjectableValueId();
        if (injectableValueId != null) {
            return _context.findInjectableValue(prop.getInjectableValueId(),
                    prop, null);
        }

        JsonRequired requiredAnnotation = prop.getAnnotation(JsonRequired.class);

        if (prop.isRequired() || (requiredAnnotation != null)) {

            if (requiredAnnotation == null) {
                requiredAnnotation = JsonConstraints.required(validationAnnotation.requiredMessage());
            }
            ConstraintDescriptor<?> constraintDescriptor =
                    new JsonRequiredConstraintDescriptor(requiredAnnotation);

            ConstraintViolation<?> violation = ConstraintViolationUtils.create(
                    null, beanType.getRawClass(), null,
                    PropertyPathUtils.constructPropertyPath(prop, features), null,
                    constraintDescriptor, messageInterpolator);

            assignViolations(prop, Collections.singleton(violation));
        }

        try {
            // Third: default value
            JsonDeserializer<Object> deser = prop.getValueDeserializer();
            return deser.getNullValue(_context);

        } catch (MismatchedInputException ex) {
            // If we get this exception, it probably means that the property type is primitive and the
            // feature FAIL_ON_NULL_FOR_PRIMITIVES is switched on. Suppress this (we already have a violation)
            // and just return the default value for the type (e.g. 0 for int)
            return ClassUtil.defaultValue(prop.getType().getRawClass());
        }
    }


    Map<Integer, Set<ConstraintViolation<?>>> getParameterViolations() {
        return parameterViolations != null ? Collections.unmodifiableMap(parameterViolations) : Collections.emptyMap();
    }
}
