package org.unbrokendome.jackson.beanvalidation.path;

import javax.validation.Path;


public final class PathUtils {

    public static Path.Node lastNode(Path path) {
        Path.Node lastNode = null;
        for (Path.Node node : path) {
            lastNode = node;
        }
        return lastNode;
    }


    public static Path simplePath(String propertyName) {
        return PathBuilder.create()
                .appendProperty(propertyName)
                .build();
    }
}
