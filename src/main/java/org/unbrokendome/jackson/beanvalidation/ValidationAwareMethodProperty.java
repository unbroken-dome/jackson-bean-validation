package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;

import javax.validation.Validator;


public class ValidationAwareMethodProperty extends AbstractValidationAwareProperty<MethodProperty> {

    public ValidationAwareMethodProperty(SettableBeanProperty src, Validator validator) {
        super(src, validator);
    }

    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty delegate) {
        return new ValidationAwareMethodProperty(delegate, validator);
    }
}
