package org.unbrokendome.jackson.beanvalidation;

import javax.validation.Validator;

import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;


public class ValidationAwareFieldProperty extends AbstractValidationAwareProperty<FieldProperty> {

    public ValidationAwareFieldProperty(SettableBeanProperty src, Validator validator) {
        super(src, validator);
    }

    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty delegate) {
        return new ValidationAwareFieldProperty(delegate, validator);
    }
}
