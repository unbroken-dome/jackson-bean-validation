package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;


final class PropertyNode extends AbstractNode implements Path.PropertyNode {

    private final Integer index;
    private final Object key;


    PropertyNode(String name, @Nullable Integer index, @Nullable Object key) {
        super(name);
        this.index = index;
        this.key = key;
    }


    @Override
    @Nonnull
    public ElementKind getKind() {
        return ElementKind.PROPERTY;
    }


    @Override
    @Nullable
    public Integer getIndex() {
        return index;
    }


    @Override
    @Nullable
    public Object getKey() {
        return key;
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
