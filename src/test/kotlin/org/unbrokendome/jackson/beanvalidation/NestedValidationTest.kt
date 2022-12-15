package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import java.util.stream.Stream
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull


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

    @JsonValidated
    class ValidatedBeanWithValidNestedList {
        var nested: List<SimpleValidatedBean>? = null
    }

    private class NestedListArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
                Arguments.of("nested[0].value", """{ "nested": [{"value": null}] }"""),
                Arguments.of("nested[1].value", """{ "nested": [{"value":"test"},{"value": null}] }"""),
                Arguments.of("nested[2].value", """{ "nested": [{"value":"1"},{"value":"2"},{"value": null}] }""")
        )
    }

    @ParameterizedTest
    @ArgumentsSource(NestedListArgumentsProvider::class)
    fun `should report violation in @Valid-annotated nested list`(violationPath: String, json: String) {
        val objectMapper = ObjectMapper()
                .registerModule(beanValidationModule)
                .configure(DeserializationFeature.WRAP_EXCEPTIONS, false)

        val violations = assertViolations {
            objectMapper.readValue(json, ValidatedBeanWithValidNestedList::class.java)
        }

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>(violationPath)
    }
}
