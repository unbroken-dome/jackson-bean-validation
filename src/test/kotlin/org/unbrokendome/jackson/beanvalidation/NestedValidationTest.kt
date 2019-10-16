package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import javax.validation.Valid
import javax.validation.constraints.NotNull


class NestedValidationTest : AbstractValidationTest() {

    class SimpleBean {

        @get:NotNull
        var value: String? = null
    }


    @JsonValidated
    class SimpleValidatedBean {

        @get:NotNull
        var value: String? = null
    }


    @JsonValidated
    class ValidatedBeanWithNested {

        var nested: SimpleBean? = null
    }



    @Test
    fun `should not report violation in not annotated nested bean`() {

        val json = """{ "nested": { "value": null } }"""

        objectMapper.readValue(json, ValidatedBeanWithNested::class.java)
    }


    @JsonValidated
    class ValidatedBeanWithValidatedNested {

        var nested: SimpleValidatedBean? = null
    }


    @Test
    fun `should report violation in annotated nested bean`() {

        val json = """{ "nested": { "value": null } }"""

        val violations = assertViolationsOnDeserialization(
            json, ValidatedBeanWithValidatedNested::class.java
        )

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("nested.value")
    }


    @JsonValidated
    class ValidatedBeanWithValidNested {

        @get:Valid
        var nested: SimpleBean? = null
    }


    @Test
    fun `should report violation in @Valid-annotated nested bean`() {

        val json = """{ "nested": { "value": null } }"""

        val violations = assertViolationsOnDeserialization(
            json, ValidatedBeanWithValidNested::class.java
        )

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("nested.value")
    }
}
