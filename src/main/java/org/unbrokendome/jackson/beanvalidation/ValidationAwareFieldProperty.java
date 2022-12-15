package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;

import jakarta.validation.Validator;


final class ValidationAwareFieldProperty extends ValidationAwareBeanProperty<FieldProperty> {

    ValidationAwareFieldProperty(
            SettableBeanProperty src, Validator validator, boolean validationEnabled) {
        super(src, validator, validationEnabled);
    }


    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty delegate, boolean validationEnabled) {
        return new ValidationAwareFieldProperty(delegate, validator, validationEnabled);
    }
}
