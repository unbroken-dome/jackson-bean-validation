package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.validation.Payload;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;


/**
 * The annotated property or parameter must be a valid JSON input; i.e. not throw a {@link MismatchedInputException}
 * on deserialization.
 * <p>
 * Note that a violation on this constraint will automatically be reported if a {@link MismatchedInputException}
 * is thrown on deserialization; you can place this annotation explicitly if you need to customize the message,
 * groups, or payload.
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonValidInput {

    String DEFAULT_MESSAGE = "{org.unbrokendome.jackson.beanvalidation.JsonValidInput.message}";

    String message() default DEFAULT_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}



/**
 * Specialized {@link ConstraintDescriptor} for violations to {@link JsonValidInput}, which originate from
 * a {@link MismatchedInputException} thrown during deserialization.
 */
class JsonValidInputConstraintDescriptor
        extends AbstractConstraintDescriptor<JsonValidInput> {

    JsonValidInputConstraintDescriptor(JsonValidInput annotation) {
        super(annotation);
    }


    @Override
    protected String getMessageFromAnnotation(JsonValidInput annotation) {
        return annotation.message();
    }


    @Override
    protected Class<?>[] getGroupsFromAnnotation(JsonValidInput annotation) {
        return annotation.groups();
    }


    @Override
    protected Class<? extends Payload>[] getPayloadFromAnnotation(JsonValidInput annotation) {
        return annotation.payload();
    }
}
