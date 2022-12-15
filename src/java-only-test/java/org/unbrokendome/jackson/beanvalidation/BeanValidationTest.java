package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.unbrokendome.jackson.beanvalidation.BeanValidationModule;
import org.unbrokendome.jackson.beanvalidation.JsonValidated;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;

class BeanValidationTest {

    @Test
    void testBeanValidation() throws Exception {
        String invalidJson = "{\"not_null\": null}";

        ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new BeanValidationModule(validatorFactory))
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        Assertions.assertThrows(ConstraintViolationException.class, () -> {
            objectMapper.readValue(invalidJson, TestSerializable.class);
        });
    }

    @JsonValidated
    public static class TestSerializable {
        @NotNull
        private final String notNull;

        @JsonCreator
        public TestSerializable(
                @NotNull @JsonProperty(value = "not_null", required = true) String notNull
        ) {
            this.notNull = notNull;
        }
    }
}