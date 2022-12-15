package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;


final class ParameterNode extends AbstractNode implements Path.ParameterNode {

    private final int parameterIndex;


    ParameterNode(String name, int parameterIndex) {
        super(name);
        this.parameterIndex = parameterIndex;
    }


    @Override
    @Nonnull
    public ElementKind getKind() {
        return ElementKind.PARAMETER;
    }


    @Override
    public int getParameterIndex() {
        return parameterIndex;
    }
}
