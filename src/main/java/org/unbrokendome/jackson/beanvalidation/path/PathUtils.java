package org.unbrokendome.jackson.beanvalidation.path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Iterator;


public final class PathUtils {

    @Nullable
    public static Path.Node firstNodeOfKind(Path path, ElementKind kind) {
        for (Path.Node node : path) {
            if (node.getKind() == kind) {
                return node;
            }
        }
        return null;
    }


    @Nonnull
    public static Path takeUntil(Path path, ElementKind kind) {
        PathBuilder builder = PathBuilder.create();
        for (Path.Node node : path) {
            builder.appendNode(node);
            if (node.getKind() == kind) {
                break;
            }
        }
        return builder.build();
    }


    @Nonnull
    public static Path dropUntil(Path path, ElementKind kind) {
        PathBuilder builder = PathBuilder.create();

        Iterator<Path.Node> iterator = path.iterator();

        while (iterator.hasNext()) {
            Path.Node node = iterator.next();

            if (node.getKind() == kind) {
                while (iterator.hasNext()) {
                    builder.appendNode(iterator.next());
                }
                break;
            }
        }

        return builder.build();
    }
}
