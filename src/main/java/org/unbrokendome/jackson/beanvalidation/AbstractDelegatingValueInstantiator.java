package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;

import java.io.IOException;


abstract class AbstractDelegatingValueInstantiator extends ValueInstantiator {

    private final ValueInstantiator delegate;


    AbstractDelegatingValueInstantiator(ValueInstantiator delegate) {
        this.delegate = delegate;
    }


    @Override
    public Class<?> getValueClass() {
        return delegate.getValueClass();
    }


    @Override
    public String getValueTypeDesc() {
        return delegate.getValueTypeDesc();
    }


    @Override
    public boolean canInstantiate() {
        return delegate.canInstantiate();
    }


    @Override
    public boolean canCreateFromString() {
        return delegate.canCreateFromString();
    }


    @Override
    public boolean canCreateFromInt() {
        return delegate.canCreateFromInt();
    }


    @Override
    public boolean canCreateFromLong() {
        return delegate.canCreateFromLong();
    }


    @Override
    public boolean canCreateFromDouble() {
        return delegate.canCreateFromDouble();
    }


    @Override
    public boolean canCreateFromBoolean() {
        return delegate.canCreateFromBoolean();
    }


    @Override
    public boolean canCreateUsingDefault() {
        return delegate.canCreateUsingDefault();
    }


    @Override
    public boolean canCreateUsingDelegate() {
        return delegate.canCreateUsingDelegate();
    }


    @Override
    public boolean canCreateUsingArrayDelegate() {
        return delegate.canCreateUsingArrayDelegate();
    }


    @Override
    public boolean canCreateFromObjectWith() {
        return delegate.canCreateFromObjectWith();
    }


    @Override
    public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
        return delegate.getFromObjectArguments(config);
    }


    @Override
    public JavaType getDelegateType(DeserializationConfig config) {
        return delegate.getDelegateType(config);
    }


    @Override
    public JavaType getArrayDelegateType(DeserializationConfig config) {
        return delegate.getArrayDelegateType(config);
    }


    @Override
    public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
        return delegate.createUsingDefault(ctxt);
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
        return delegate.createFromObjectWith(ctxt, args);
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, SettableBeanProperty[] props, PropertyValueBuffer buffer) throws IOException {
        return delegate.createFromObjectWith(ctxt, props, buffer);
    }


    @Override
    public Object createUsingDelegate(DeserializationContext ctxt, Object delegate) throws IOException {
        return this.delegate.createUsingDelegate(ctxt, delegate);
    }


    @Override
    public Object createUsingArrayDelegate(DeserializationContext ctxt, Object delegate) throws IOException {
        return this.delegate.createUsingArrayDelegate(ctxt, delegate);
    }


    @Override
    public Object createFromString(DeserializationContext ctxt, String value) throws IOException {
        return delegate.createFromString(ctxt, value);
    }


    @Override
    public Object createFromInt(DeserializationContext ctxt, int value) throws IOException {
        return delegate.createFromInt(ctxt, value);
    }


    @Override
    public Object createFromLong(DeserializationContext ctxt, long value) throws IOException {
        return delegate.createFromLong(ctxt, value);
    }


    @Override
    public Object createFromDouble(DeserializationContext ctxt, double value) throws IOException {
        return delegate.createFromDouble(ctxt, value);
    }


    @Override
    public Object createFromBoolean(DeserializationContext ctxt, boolean value) throws IOException {
        return delegate.createFromBoolean(ctxt, value);
    }


    @Override
    public AnnotatedWithParams getDefaultCreator() {
        return delegate.getDefaultCreator();
    }


    @Override
    public AnnotatedWithParams getDelegateCreator() {
        return delegate.getDelegateCreator();
    }


    @Override
    public AnnotatedWithParams getArrayDelegateCreator() {
        return delegate.getArrayDelegateCreator();
    }


    @Override
    public AnnotatedWithParams getWithArgsCreator() {
        return delegate.getWithArgsCreator();
    }


    @Override
    public AnnotatedParameter getIncompleteParameter() {
        return delegate.getIncompleteParameter();
    }
}
