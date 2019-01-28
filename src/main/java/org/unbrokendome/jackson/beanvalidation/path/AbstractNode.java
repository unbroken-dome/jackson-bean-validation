package org.unbrokendome.jackson.beanvalidation.path;

import javax.validation.ElementKind;
import javax.validation.Path;

public abstract class AbstractNode implements Path.Node {

    private final String name;

    protected AbstractNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T extends Path.Node> T as(Class<T> nodeType) {
        return nodeType.cast(this);
    }

    @Override
    public boolean isInIterable() {
        return false;
    }

    @Override
    public Integer getIndex() {
        return null;
    }

    @Override
    public Object getKey() {
        return null;
    }
}
