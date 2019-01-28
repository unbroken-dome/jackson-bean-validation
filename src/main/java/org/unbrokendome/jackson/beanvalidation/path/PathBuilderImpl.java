package org.unbrokendome.jackson.beanvalidation.path;

import java.util.Deque;
import java.util.LinkedList;

import javax.validation.ElementKind;
import javax.validation.Path;

public class PathBuilderImpl implements PathBuilder {

    private final Deque<Path.Node> nodes = new LinkedList<>();

    @Override
    public PathBuilder appendPath(Path path) {
        path.forEach(node -> {
            if (node.getKind() != ElementKind.PROPERTY) {
                throw new IllegalArgumentException("Path supports only PROPERTY nodes");
            }
            if (node.getIndex() != null) {
                appendIndexedProperty(node.getName(), node.getIndex());
            } else if (node.getKey() != null) {
                appendKeyedProperty(node.getName(), node.getKey());
            } else {
                appendProperty(node.getName());
            }
        });
        return this;
    }


    @Override
    public PathBuilder appendProperty(String name) {
        nodes.addLast(new SimplePropertyNode(name));
        return this;
    }

    @Override
    public PathBuilder appendIndexedProperty(String name, int index) {
        nodes.addLast(new IndexedPropertyNode(name, index));
        return this;
    }

    @Override
    public PathBuilder appendKeyedProperty(String name, Object key) {
        nodes.addLast(new KeyedPropertyNode(name, key));
        return this;
    }

    @Override
    public Path build() {
        return new PathImpl(nodes);
    }
}
