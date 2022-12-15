package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;


final class BeanNode extends AbstractNode implements Path.BeanNode {

    private static final BeanNode INSTANCE = new BeanNode();


    private BeanNode() {
        super("");
    }


    static BeanNode getInstance() {
        return INSTANCE;
    }


    @Override
    @Nonnull
    public ElementKind getKind() {
        return ElementKind.BEAN;
    }


    @Override
    @Nullable
    public Class<?> getContainerClass() {
        return null;
    }


    @Override
    @Nullable
    public Integer getTypeArgumentIndex() {
        return null;
    }
}
