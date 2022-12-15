package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.Path;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("UnusedReturnValue")
public interface PathBuilder {

    @Nonnull
    PathBuilder appendPath(Path path);

    @Nonnull
    PathBuilder appendNode(Path.Node node);

    @Nonnull
    PathBuilder appendBeanNode();

    @Nonnull
    PathBuilder appendConstructor(String name, List<Class<?>> parameterTypes);

    @Nonnull
    default PathBuilder appendConstructor(Constructor<?> constructor) {
        return appendConstructor(constructor.getName(), Arrays.asList(constructor.getParameterTypes()));
    }

    @Nonnull
    default PathBuilder appendProperty(String name) {
        return appendProperty(name, null, null);
    }


    @Nonnull
    PathBuilder appendProperty(String name, @Nullable Integer index, @Nullable Object key);


    @Nonnull
    PathBuilder appendParameter(String name, int parameterIndex);

    @Nonnull
    Path build();

    @Nonnull
    static PathBuilder create() {
        return new PathBuilderImpl();
    }
}
