package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


final class PathBuilderImpl implements PathBuilder {

    private final Deque<Path.Node> nodes = new LinkedList<>();


    @Nonnull
    @Override
    public PathBuilder appendPath(Path path) {
        Iterator<Path.Node> pathIterator = path.iterator();
        if (!pathIterator.hasNext()) {
            // other path is empty
            return this;
        }

        Path.Node firstNode = pathIterator.next();
        // Don't duplicate the starting BEAN node
        if (nodes.isEmpty() || firstNode.getKind() != ElementKind.BEAN) {
            nodes.addLast(firstNode);
        }

        while (pathIterator.hasNext()) {
            nodes.addLast(pathIterator.next());
        }

        return this;
    }


    @Nonnull
    @Override
    public PathBuilder appendNode(Path.Node node) {
        nodes.addLast(node);
        return this;
    }


    @Nonnull
    public PathBuilder appendBeanNode() {
        return appendNode(BeanNode.getInstance());
    }


    @Nonnull
    @Override
    public PathBuilder appendConstructor(String name, List<Class<?>> parameterTypes) {
        return appendNode(new ConstructorNode(name, parameterTypes));
    }


    @Nonnull
    @Override
    public PathBuilder appendProperty(String name, @Nullable Integer index, @Nullable Object key) {
        return appendNode(new PropertyNode(name, index, key));
    }


    @Nonnull
    @Override
    public PathBuilder appendParameter(String name, int parameterIndex) {
        nodes.addLast(new ParameterNode(name, parameterIndex));
        return this;
    }


    @Nonnull
    @Override
    public Path build() {
        return new PathImpl(nodes);
    }
}
