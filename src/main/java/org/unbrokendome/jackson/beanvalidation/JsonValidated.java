package org.unbrokendome.jackson.beanvalidation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonValidated {

    String validInputMessage() default JsonValidInput.DEFAULT_MESSAGE;

    String requiredMessage() default JsonRequired.DEFAULT_MESSAGE;
}
