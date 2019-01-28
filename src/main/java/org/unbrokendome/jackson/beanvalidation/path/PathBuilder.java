package org.unbrokendome.jackson.beanvalidation.path;

import javax.validation.Path;

@SuppressWarnings("UnusedReturnValue")
public interface PathBuilder {

    PathBuilder appendPath(Path path);

    PathBuilder appendProperty(String name);

    PathBuilder appendIndexedProperty(String name, int index);

    PathBuilder appendKeyedProperty(String name, Object key);

    Path build();

    static PathBuilder create() {
        return new PathBuilderImpl();
    }

    static PathBuilder fromPath(Path path) {
        return create().appendPath(path);
    }
}
