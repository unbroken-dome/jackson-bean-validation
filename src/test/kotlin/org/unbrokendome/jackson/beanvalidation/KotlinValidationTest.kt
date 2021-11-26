package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import java.util.stream.Stream
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import javax.validation.constraints.NotNull
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass


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


    @Test
    fun `should throw UnrecognizedPropertyException for object`() {

        val json = """{ "left": "abc", "right": 42, "other": "o" }"""

        assertThrows<UnrecognizedPropertyException> {
            objectMapper.readValue(json, BeanWithNotNullCreatorProps::class.java)
        }
    }


    @JsonValidated
    data class Container(val list: List<Item>)

    @JsonValidated
    data class Item(val value: String)

    @Test
    fun `should throw UnrecognizedPropertyException for nested list`() {

        val json = """{ "list": [{"value": "test", "other": "o"}, {"value": "abc"}] }"""

        assertThrows<UnrecognizedPropertyException> {
            objectMapper.readValue(json, Container::class.java)
        }
    }


    @JsonValidated
    data class ValidatedDataWithValidNestedList(val nested: List<SimpleData>)

    @JsonValidated
    data class SimpleData(val value: String)

    private class NestedListArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> = Stream.of(
                Arguments.of("nested[0].value", """{ "nested": [{}, {"value":"v1"}] }"""),
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

        @Test
        fun `should report violation on all records in @Valid-annotated nested list`() {

            val json = """{ "nested": [{}, {"value":"2"}, {"value":null}, {"value":"4"}] }"""

            val violations = assertViolationsOnDeserialization<ValidatedDataWithValidNestedList>(json)

            assertThat(violations).hasSize(2)
            assertThat(violations).hasViolation<JsonRequired>("nested[0].value")
            assertThat(violations).hasViolation<NotNull>("nested[2].value")
        }
    }


    @JsonValidated
    data class ContainerIndexBean(@UniqueData val nested: List<SimpleData>)

    @Target(VALUE_PARAMETER)
    @Constraint(validatedBy = [UniqueDataValidator::class])
    annotation class UniqueData(
        val message: String = "Duplicated value",
        val groups: Array<KClass<*>> = [],
        val payload: Array<KClass<out Payload>> = [],
    )

    class UniqueDataValidator : ConstraintValidator<UniqueData, List<SimpleData>> {
        override fun isValid(list: List<SimpleData>, context: ConstraintValidatorContext): Boolean {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Duplicated value")
                .addPropertyNode("value")
                .inContainer(List::class.java, 0)
                .inIterable().atIndex(1)
                .addConstraintViolation()
            return false
        }
    }

    @Test
    fun `should report violation on container element node`() {

        val json = """{ "nested": [{"value":"test"}, {"value":"test"}] }"""

        val violations = assertViolationsOnDeserialization<ContainerIndexBean>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<UniqueData>("nested[1].value")
    }
}
