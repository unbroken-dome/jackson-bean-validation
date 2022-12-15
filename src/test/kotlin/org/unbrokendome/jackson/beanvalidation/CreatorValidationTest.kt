package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull


class CreatorValidationTest : AbstractValidationTest() {

    class SimpleBean
    @JsonCreator constructor(
        @param:[NotNull JsonProperty("value")] val value: String?
    )

    @JsonValidated
    class SimpleValidatedBean
    @JsonCreator constructor(
        @param:[NotNull JsonProperty("value")] val value: String
    )

    @JsonValidated
    class SimpleValidatedBeanWithRequired
    @JsonCreator constructor(
        @param:JsonProperty("value", required = true) val value: String?
    )

    @JsonValidated
    class SimpleValidatedBeanWithRequiredNotNull
    @JsonCreator constructor(
        @param:[NotNull JsonProperty("value", required = true)] val value: String
    )

    @JsonValidated
    class ValidatedBeanWithNested
    @JsonCreator constructor(
        @param:JsonProperty("nested") val nested: SimpleValidatedBean
    )

    @JsonValidated
    class ValidatedBeanWithNestedRequired
    @JsonCreator constructor(
        @param:JsonProperty("nested") val nested: SimpleValidatedBeanWithRequired
    )

    @JsonValidated
    class ValidatedBeanWithValidatedNested
    @JsonCreator constructor(
        @param:[JsonValidated JsonProperty("nested")] val nested: SimpleBean
    )

    @JsonValidated
    class ValidatedBeanWithValidNested
    @JsonCreator constructor(
        @param:[Valid JsonProperty("nested")] val nested: SimpleBean
    )


    @Test
    fun `should report NotNull violation on null creator property`() {

        val json = """{ "value": null }"""
        val violations = assertViolationsOnDeserialization<SimpleValidatedBean>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun `should report JsonRequired violation on missing required creator property`() {
        val json = "{ }"
        val violations = assertViolationsOnDeserialization<SimpleValidatedBeanWithRequired>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<JsonRequired>("value")
    }


    @Test
    fun `should report only NotNull violation on null required creator property`() {
        val json = """{ "value": null }"""
        val violations = assertViolationsOnDeserialization<SimpleValidatedBeanWithRequiredNotNull>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun `should report JsonRequired violation on nested bean creator property`() {
        val json = """{ "nested": { } }"""
        val violations = assertViolationsOnDeserialization<ValidatedBeanWithNestedRequired>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<JsonRequired>("nested.value")
    }


    @Test
    fun `should report NotNull violation on nested bean creator property`() {
        val json = """{ "nested": { "value": null } }"""
        val violations = assertViolationsOnDeserialization<ValidatedBeanWithNested>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("nested.value")
    }


    @Test
    fun `should report NotNull violation on property-annotated nested bean`() {
        val json = """{ "nested": { "value": null } }"""
        val violations = assertViolationsOnDeserialization<ValidatedBeanWithValidatedNested>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("nested.value")
    }


    @Test
    fun `should report NotNull violation on @Valid property-annotated nested bean`() {
        val json = """{ "nested": { "value": null } }"""
        val violations = assertViolationsOnDeserialization<ValidatedBeanWithValidNested>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("nested.value")
    }


    /**
     * Bean that has one property passed to the constructor, and one settable property
     */
    @JsonValidated
    class BeanWithCreatorParamAndProperty
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
        @param:[NotNull JsonProperty("left")] val left: String
    ) {

        @get:NotNull
        var right: String? = null
    }


    @Test
    fun `should validate setter properties not explicitly set`() {

        val json = """{ "left": "123" }"""

        val violations = assertViolationsOnDeserialization(json, BeanWithCreatorParamAndProperty::class.java)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("right")
    }
}
