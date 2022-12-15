package org.unbrokendome.jackson.beanvalidation;

import javax.annotation.Nullable;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Annotation;


@SuppressWarnings("ClassExplicitlyAnnotation")
final class JsonConstraints {

    private JsonConstraints() {
    }


    private static abstract class AbstractDefaultAnnotation {

        public Class<?>[] groups() {
            return new Class<?>[0];
        }


        @SuppressWarnings("unchecked")
        public Class<? extends Payload>[] payload() {
            return new Class[0];
        }
    }


    private static final class DefaultJsonValidInput
            extends AbstractDefaultAnnotation implements JsonValidInput {

        private final String message;


        private DefaultJsonValidInput(String message) {
            this.message = message;
        }


        @Override
        public String message() {
            return message;
        }


        @Override
        public Class<? extends Annotation> annotationType() {
            return JsonValidInput.class;
        }
    }


    private static final JsonValidInput DEFAULT_VALID_INPUT =
            new DefaultJsonValidInput(JsonValidInput.DEFAULT_MESSAGE);


    static JsonValidInput validInput(@Nullable String messageTemplate) {
        if (messageTemplate == null || messageTemplate.equals(JsonValidInput.DEFAULT_MESSAGE)) {
            return DEFAULT_VALID_INPUT;
        } else {
            return new DefaultJsonValidInput(messageTemplate);
        }
    }


    private static final class DefaultJsonRequired
            extends AbstractDefaultAnnotation implements JsonRequired {

        private final String message;


        private DefaultJsonRequired(String message) {
            this.message = message;
        }


        @Override
        public String message() {
            return message;
        }


        @Override
        public Class<? extends Annotation> annotationType() {
            return JsonRequired.class;
        }
    }


    private static final JsonRequired DEFAULT_JSON_REQUIRED =
            new DefaultJsonRequired(JsonRequired.DEFAULT_MESSAGE);


    static JsonRequired required(@Nullable String messageTemplate) {
        if (messageTemplate == null || messageTemplate.equals(JsonRequired.DEFAULT_MESSAGE)) {
            return DEFAULT_JSON_REQUIRED;
        } else {
            return new DefaultJsonRequired(messageTemplate);
        }
    }


    private static final class DefaultNotNull
            extends AbstractDefaultAnnotation implements NotNull {

        private static final String DEFAULT_MESSAGE = "{jakarta.validation.constraints.NotNull.message}";

        @Override
        public String message() {
            return DEFAULT_MESSAGE;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return NotNull.class;
        }
    }


    private static final NotNull DEFAULT_NOT_NULL = new DefaultNotNull();


    static NotNull notNull() {
        return DEFAULT_NOT_NULL;
    }
}
