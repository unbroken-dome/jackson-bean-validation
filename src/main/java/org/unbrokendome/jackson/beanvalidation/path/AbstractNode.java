package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.Path;


abstract class AbstractNode implements Path.Node {

    private final String name;


    AbstractNode(String name) {
        this.name = name;
    }


    @Override
    @Nonnull
    public String getName() {
        return name;
    }


    @Override
    @Nonnull
    public <T extends Path.Node> T as(Class<T> nodeType) {
        return nodeType.cast(this);
    }


    @Override
    public boolean isInIterable() {
        return false;
    }


    @Override
    @Nullable
    public Integer getIndex() {
        return null;
    }


    @Override
    @Nullable
    public Object getKey() {
        return null;
    }


    @Override
    @Nonnull
    public String toString() {
        return name;
    }
}
