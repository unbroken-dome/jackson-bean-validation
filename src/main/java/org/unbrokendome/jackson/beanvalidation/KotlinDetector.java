package org.unbrokendome.jackson.beanvalidation;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;


final class KotlinDetector {

    @Nullable
    private static final Class<? extends Annotation> kotlinMetadata;
    private static final boolean kotlinReflectPresent;


    static {
        kotlinMetadata = findKotlinMetadataClass();
        kotlinReflectPresent = kotlinMetadata != null && checkIfKotlinReflectIsPresent();
    }


    @Nullable
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> findKotlinMetadataClass() {
        try {
            return (Class<? extends Annotation>) Class.forName("kotlin.Metadata");
        }
        catch (ClassNotFoundException ex) {
            // Kotlin API not available - no Kotlin support
            return null;
        }
    }


    private static boolean checkIfKotlinReflectIsPresent() {
        try {
            Class.forName("kotlin.reflect.full.KClasses");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }


    /**
     * Determine whether Kotlin is present in general.
     */
    static boolean isKotlinPresent() {
        return (kotlinMetadata != null);
    }


    /**
     * Determine whether Kotlin reflection is present.
     */
    static boolean isKotlinReflectPresent() {
        return kotlinReflectPresent;
    }


    /**
     * Determine whether the given {@code Class} is a Kotlin type
     * (with Kotlin metadata present on it).
     */
    static boolean isKotlinType(Class<?> clazz) {
        return (kotlinMetadata != null && clazz.getDeclaredAnnotation(kotlinMetadata) != null);
    }
}
