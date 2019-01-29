package org.unbrokendome.jackson.beanvalidation.violation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


abstract class AbstractRootedConstraintViolation<T> extends AbstractConstraintViolation<T> {

    @Nullable
    private final T rootBean;

    @Nonnull
    private final Class<T> rootBeanClass;


    AbstractRootedConstraintViolation(@Nullable T rootBean, @Nonnull Class<T> rootBeanClass) {
        this.rootBean = rootBean;
        this.rootBeanClass = rootBeanClass;
    }


    @Override
    @Nullable
    public final T getRootBean() {
        return rootBean;
    }


    @Override
    @Nonnull
    public final Class<T> getRootBeanClass() {
        return rootBeanClass;
    }
}
