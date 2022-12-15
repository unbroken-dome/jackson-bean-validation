package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import jakarta.validation.Path;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;


class PathImpl implements Path {

    private final Deque<Node> nodes;


    PathImpl(Iterable<Path.Node> nodes) {
        this.nodes = new LinkedList<>();
        nodes.forEach(this.nodes::add);
    }


    @Override
    @Nonnull
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }


    @Override
    @Nonnull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {

            String nodeValue = node.toString();
            if (nodeValue == null || nodeValue.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(nodeValue);
        }
        return builder.toString();
    }
}
