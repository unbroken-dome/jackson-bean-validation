package org.unbrokendome.jackson.beanvalidation;

import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;

import javax.validation.ConstraintViolation;
import javax.validation.Path;


final class ConstraintViolationUtils {

    static <T> ConstraintViolation<T> copyWithNewPath(ConstraintViolation<T> violation, Path newPath) {
        return new ConstraintViolationImpl<>(
                violation.getMessage(),
                violation.getRootBean(),
                violation.getRootBeanClass(),
                violation.getLeafBean(),
                newPath,
                violation.getInvalidValue(),
                violation.getConstraintDescriptor());
    }


    static <T> ConstraintViolation<T> resolvePath(ConstraintViolation<T> violation, String basePath) {

        Path newPath = PathBuilder.create()
                .appendProperty(basePath)
                .appendPath(violation.getPropertyPath())
                .build();

        return copyWithNewPath(violation, newPath);
    }
}
