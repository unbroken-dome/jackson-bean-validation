package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;

import jakarta.validation.Validator;


final class ValidationAwareMethodProperty extends ValidationAwareBeanProperty<MethodProperty> {

    ValidationAwareMethodProperty(
            SettableBeanProperty src, Validator validator, boolean validationEnabled
    ) {
        super(src, validator, validationEnabled);
    }


    @Override
    protected SettableBeanProperty withDelegate(SettableBeanProperty delegate, boolean validationEnabled) {
        return new ValidationAwareMethodProperty(delegate, validator, validationEnabled);
    }
}
