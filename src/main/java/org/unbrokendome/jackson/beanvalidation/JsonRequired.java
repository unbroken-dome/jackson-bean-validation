package org.unbrokendome.jackson.beanvalidation;

import jakarta.validation.Payload;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;


/**
 * The annotated property or parameter must be present on input. This is equivalent to placing
 * {@code @JsonProperty(required=true)} on the element.
 * <p>
 * Note that a violation on this constraint will automatically be reported if a required property is missing
 * on deserialization; you can place this annotation explicitly if you need to customize the message, groups,
 * or payload.
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonRequired {

    String DEFAULT_MESSAGE = "{org.unbrokendome.jackson.beanvalidation.JsonRequired.message}";

    String message() default DEFAULT_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}


/**
 * Specialized {@link ConstraintDescriptor} for violations to {@link JsonRequired}, which originate from
 * a missing required value during deserialization.
 */
final class JsonRequiredConstraintDescriptor
        extends AbstractConstraintDescriptor<JsonRequired> {

    JsonRequiredConstraintDescriptor(JsonRequired annotation) {
        super(annotation);
    }


    @Override
    protected String getMessageFromAnnotation(JsonRequired annotation) {
        return annotation.message();
    }


    @Override
    protected Class<?>[] getGroupsFromAnnotation(JsonRequired annotation) {
        return annotation.groups();
    }


    @Override
    protected Class<? extends Payload>[] getPayloadFromAnnotation(JsonRequired annotation) {
        return annotation.payload();
    }
}
