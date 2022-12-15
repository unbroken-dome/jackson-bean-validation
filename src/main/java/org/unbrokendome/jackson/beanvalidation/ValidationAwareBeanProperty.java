package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;


@SuppressWarnings("BooleanMethodIsAlwaysInverted")
abstract class ValidationAwareBeanProperty<P extends SettableBeanProperty>
        extends SettableBeanProperty {

    private static final Object SKIP_NULL_VALUE = new Object();

    protected final P delegate;
    protected final Validator validator;
    private boolean validationEnabled;


    @SuppressWarnings("unchecked")
    protected ValidationAwareBeanProperty(SettableBeanProperty src, Validator validator, boolean validationEnabled) {
        super(src);
        this.delegate = (P) src;
        this.validator = validator;
        this.validationEnabled = validationEnabled;
    }


    protected abstract SettableBeanProperty withDelegate(SettableBeanProperty delegate, boolean validationEnabled);


    @Override
    public final SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return withDelegate(delegate.withValueDeserializer(deser), validationEnabled);
    }


    @Override
    public final SettableBeanProperty withName(PropertyName newName) {
        return withDelegate(delegate.withName(newName), validationEnabled);
    }


    @Override
    public final SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return withDelegate(delegate.withNullProvider(nva), validationEnabled);
    }


    void enableValidation() {
        this.validationEnabled = true;
    }


    protected String getBeanPropertyName() {
        return PropertyUtils.getPropertyNameFromMember(getMember());
    }


    @Override
    public final AnnotatedMember getMember() {
        return delegate.getMember();
    }


    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> acls) {
        return delegate.getAnnotation(acls);
    }


    protected boolean shouldSkipNulls() {
        return NullsConstantProvider.isSkipper(_nullProvider);
    }


    private Object deserializeValue(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_NULL) && shouldSkipNulls()) {
            return SKIP_NULL_VALUE;
        }
        return deserialize(p, ctxt);
    }


    private boolean validatePropertyOnInvalidOject(
            JsonParser p, DeserializationContext ctxt, Object instance
    ) throws IOException {
        if (validationEnabled && instance instanceof InvalidObject) {
            // Instance wasn't even created because there were already validation errors in the creator.
            // Just validate the properties but don't set them.
            Object value = deserializeValue(p, ctxt);
            if (value != SKIP_NULL_VALUE) {
                Set<? extends ConstraintViolation<?>> violations = validator.validateValue(
                        getMember().getDeclaringClass(), getBeanPropertyName(), value);
                ((InvalidObject) instance).addAdditionalViolations(violations);
            }
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
        if (!validatePropertyOnInvalidOject(p, ctxt, instance)) {
            doDeserializeAndSet(p, ctxt, instance);
        }
    }


    protected void doDeserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
        delegate.deserializeAndSet(p, ctxt, instance);
    }


    @Override
    public Object deserializeSetAndReturn(
            JsonParser p, DeserializationContext ctxt, Object instance
    ) throws IOException {
        if (!validatePropertyOnInvalidOject(p, ctxt, instance)) {
            return delegate.deserializeSetAndReturn(p, ctxt, instance);
        } else {
            return null;
        }
    }


    private boolean validateValueOnInvalidOject(Object instance, Object value) {
        if (instance instanceof InvalidObject) {
            // Instance wasn't even created because there were already validation errors in the creator.
            // Just validate the properties but don't set them.
            Set<? extends ConstraintViolation<?>> violations = validator.validateValue(
                    getMember().getDeclaringClass(), getBeanPropertyName(), value);
            ((InvalidObject) instance).addAdditionalViolations(violations);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void set(Object instance, Object value) throws IOException {
        if (!validateValueOnInvalidOject(instance, value)) {
            delegate.set(instance, value);
        }
    }


    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException {
        if (!validateValueOnInvalidOject(instance, value)) {
            return delegate.setAndReturn(instance, value);
        } else {
            return null;
        }
    }
}
