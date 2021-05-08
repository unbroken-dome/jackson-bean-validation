package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import java.util.stream.Stream
import javax.validation.constraints.NotNull


class KotlinValidationTest : AbstractValidationTest() {

    @BeforeEach
    fun registerKotlinModule() {
        objectMapper.registerModule(KotlinModule())
        objectMapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        objectMapper.configure(DeserializationFeature.WRAP_EXCEPTIONS, false)
    }

    @JsonValidated
    data class BeanWithNotNullCreatorProps(
        val left: String,
        val right: Int
    )

    @JsonValidated
    class BeanWithNotNullVarProp {
        var value: String = ""
    }

    @JsonValidated
    class BeanWithLateinitVarProp {
        lateinit var value: String
    }

    @Test
    fun `should report NotNull violation on null creator String property`() {

        val json = """{ "left": null, "right": 42 }"""

        val violations = assertViolationsOnDeserialization<BeanWithNotNullCreatorProps>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("left")
    }


    @Test
    fun `should report JsonRequired violation on missing creator String property`() {

        val json = """{ "right": 42 }"""

        val violations = assertViolationsOnDeserialization<BeanWithNotNullCreatorProps>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<JsonRequired>("left")
    }


    @Test
    fun `should report NotNull violation on null creator primitive property`() {

        val json = """{ "left": "xyz", "right": null }"""

        val violations = assertViolationsOnDeserialization<BeanWithNotNullCreatorProps>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("right")
    }


    @Test
    fun `should report JsonRequired violation on missing creator primitive property`() {

        val json = """{ "left": "xyz" }"""

        val violations = assertViolationsOnDeserialization<BeanWithNotNullCreatorProps>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<JsonRequired>("right")
    }


    @Test
    fun `should report NotNull violation on var property`() {

        val json = """{ "value": null }"""

        val violations = assertViolationsOnDeserialization<BeanWithLateinitVarProp>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun `should report NotNull violation on lateinit var property`() {

        val json = """{ "value": null }"""

        val violations = assertViolationsOnDeserialization<BeanWithLateinitVarProp>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun `should report NotNull violation on unset lateinit var property`() {

        val json = "{ }"

        val violations = assertViolationsOnDeserialization<BeanWithLateinitVarProp>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun `should not validate unset lateinit var property if feature is unset`() {

        beanValidationModule.disable(BeanValidationFeature.VALIDATE_KOTLIN_LATEINIT_VARS)

        val json = "{ }"

        assertNoViolationsOnDeserialization<BeanWithLateinitVarProp>(json)
    }

    @JsonValidated
    data class ValidatedDataWithValidNestedList(val nested: List<SimpleData>)

    @JsonValidated
    data class SimpleData(val value: String)

    private class NestedListArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
                Arguments.of("nested[0].value", """{ "nested": [{}] }"""),
                Arguments.of("nested[1].value", """{ "nested": [{"value":"test"},{}] }"""),
                Arguments.of("nested[2].value", """{ "nested": [{"value":"1"},{"value":"2"},{}] }""")
        )
    }

    @Nested
    inner class ForNestedList {

        @ParameterizedTest
        @ArgumentsSource(NestedListArgumentsProvider::class)
        fun `should report violation in @Valid-annotated nested list for readValue`(violationPath: String, json: String) {

            val violations = assertViolationsOnDeserialization<ValidatedDataWithValidNestedList>(json)

            assertThat(violations).hasSize(1)
            assertThat(violations).hasViolation<JsonRequired>(violationPath)
        }

        @ParameterizedTest
        @ArgumentsSource(NestedListArgumentsProvider::class)
        fun `should report violation in @Valid-annotated nested list for treeToValue`(violationPath: String, json: String) {

            val jsonNode = objectMapper.readTree(json)

            val violations = assertViolations {
                objectMapper.treeToValue(jsonNode, ValidatedDataWithValidNestedList::class.java)
            }

            assertThat(violations).hasSize(1)
            assertThat(violations).hasViolation<JsonRequired>(violationPath)
        }
    }
}
