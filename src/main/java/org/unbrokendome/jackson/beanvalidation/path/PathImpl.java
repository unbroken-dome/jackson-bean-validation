package org.unbrokendome.jackson.beanvalidation.path;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.validation.Path;

class PathImpl implements Path {

    private final Deque<Node> nodes;

    PathImpl(Iterable<Path.Node> nodes) {
        this.nodes = new LinkedList<>();
        nodes.forEach(this.nodes::add);
    }

    @Override
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        nodes.forEach(node -> {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(node.getName());
            if (node.getIndex() != null) {
                builder.append('[').append(node.getIndex()).append(']');
            } else if (node.getKey() != null) {
                builder.append('[').append(node.getKey()).append(']');
            }
        });
        return builder.toString();
    }
}
