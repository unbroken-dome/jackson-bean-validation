package org.unbrokendome.jackson.beanvalidation.path;

import javax.validation.ElementKind;
import javax.validation.Path;

public abstract class AbstractPropertyNode extends AbstractNode implements Path.PropertyNode {

    protected AbstractPropertyNode(String name) {
        super(name);
    }

    @Override
    public final ElementKind getKind() {
        return ElementKind.PROPERTY;
    }

    @Override
    public Class<?> getContainerClass() {
        return null;
    }

    @Override
    public Integer getTypeArgumentIndex() {
        return null;
    }
}
