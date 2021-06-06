package org.unbrokendome.jackson.beanvalidation;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Constructor;

public interface ConstructorValidatorFactory {
    Validator getValidator(ValidatorFactory validatorFactory, Constructor constructor, Object[] parameterValues);
}
