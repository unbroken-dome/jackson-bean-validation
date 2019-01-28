package org.unbrokendome.jackson.beanvalidation.path;

import javax.validation.ElementKind;
import javax.validation.Path;

public class BeanNode extends AbstractNode implements Path.BeanNode {

    public BeanNode(String name) {
        super(name);
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.BEAN;
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
