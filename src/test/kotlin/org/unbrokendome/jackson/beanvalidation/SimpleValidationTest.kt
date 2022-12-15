package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import java.io.IOException
import jakarta.validation.constraints.NotNull


class SimpleValidationTest : AbstractValidationTest() {

    class SimpleBean {

        @get:NotNull
        var value: String? = null
    }


    @Test
    fun shouldNotValidatePropertiesOnNotAnnotatedBeans() {

        val json = """{ "value": null }"""

        val simpleBean = objectMapper.readValue(json, SimpleBean::class.java)
        // even though SimpleBean has a @NotNull annotation, it should not be validated

        assertNotNull(simpleBean, "Bean should not be validated")
    }


    @JsonValidated
    class SimpleValidatedBean {

        @get:NotNull
        var value: String? = null
    }


    @Test
    fun shouldValidatePropertyOnAnnotatedBean() {

        val json = """{ "value": null }"""

        val violations = assertViolationsOnDeserialization(
            json, SimpleValidatedBean::class.java
        )

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }
}
