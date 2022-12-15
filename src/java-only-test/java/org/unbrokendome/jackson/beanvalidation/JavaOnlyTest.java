package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;

class JavaOnlyTest {

    @JsonValidated
    static class Bean {
        @JsonCreator
        public Bean(@JsonProperty("value") @NotNull String value) {
        }
    }

    private ValidatorFactory validatorFactory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();

    @Test
    void shouldWorkWithoutKotlinLibraries() {

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new BeanValidationModule(validatorFactory));

        String json = "{ \"value\": null }";

        Assertions.assertThrows(ConstraintViolationException.class,
                () -> objectMapper.readValue(json, Bean.class)
        );
    }
}
