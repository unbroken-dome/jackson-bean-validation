package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonStreamContext;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Placeholder for an object that could not be instantiated because there were constraint violations
 * on the creator parameters.
 */
final class InvalidObject {

    private Set<ConstraintViolation<?>> violations;
    private int indexInArray;


    InvalidObject(ConstraintViolationException ex, JsonStreamContext parsingContext) {
        this.violations = new LinkedHashSet<>(ex.getConstraintViolations());
        this.indexInArray = PropertyPathUtils.getIndexInArray(parsingContext);
    }


    Class<?> getType() {
        return violations.iterator().next().getRootBeanClass();
    }


    Set<ConstraintViolation<?>> getViolations() {
        return Collections.unmodifiableSet(violations);
    }

    int getIndexInArray() {
        return indexInArray;
    }

    void addAdditionalViolations(Set<? extends ConstraintViolation<?>> violations) {
        this.violations.addAll(violations);
    }
}
