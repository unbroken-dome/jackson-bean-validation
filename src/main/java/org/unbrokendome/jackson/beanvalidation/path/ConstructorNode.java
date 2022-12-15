package org.unbrokendome.jackson.beanvalidation.path;


import javax.annotation.Nonnull;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.List;


class ConstructorNode extends AbstractNode implements Path.ConstructorNode {

    private final List<Class<?>> parameterTypes;


    ConstructorNode(String name, List<Class<?>> parameterTypes) {
        super(name);
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
    }


    @Override
    @Nonnull
    public ElementKind getKind() {
        return ElementKind.CONSTRUCTOR;
    }


    @Override
    @Nonnull
    public List<Class<?>> getParameterTypes() {
        return parameterTypes;
    }


    @Override
    @Nonnull
    public String toString() {
        return "";
    }
}
