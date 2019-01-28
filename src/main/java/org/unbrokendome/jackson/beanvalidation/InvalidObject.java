package org.unbrokendome.jackson.beanvalidation;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Placeholder for an object that could not be instantiated because there were constraint violations
 * on the creator parameters.
 */
final class InvalidObject {

    private final Class<?> type;
    private Set<ConstraintViolation<?>> violations;


    InvalidObject(Class<?> type, Set<ConstraintViolation<?>> violations) {
        this.type = type;
        this.violations = new LinkedHashSet<>(violations);
    }


    Class<?> getType() {
        return type;
    }


    Set<ConstraintViolation<?>> getViolations() {
        return Collections.unmodifiableSet(violations);
    }


    void addAdditionalViolations(Set<? extends ConstraintViolation<?>> violations) {
        this.violations.addAll(violations);
    }
}
