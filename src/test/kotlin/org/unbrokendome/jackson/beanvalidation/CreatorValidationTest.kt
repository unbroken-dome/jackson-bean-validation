package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import javax.validation.constraints.NotNull


class CreatorValidationTest : AbstractValidationTest() {

    @JsonValidated
    class ImmutableValidatedBean
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
        @param:[NotNull JsonProperty("value")] val value: String
    )


    @Test
    fun shouldValidateMissingCreatorParameterOnAnnotatedBean() {

        val json = "{ }"

        val violations = assertViolationsOnDeserialization(json, ImmutableValidatedBean::class.java)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
    }


    @Test
    fun shouldValidateExplicitNullCreatorParameterOnAnnotatedBean() {

        val json = """{ "value": null }"""

        val violations = assertViolationsOnDeserialization(json, ImmutableValidatedBean::class.java)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("value")
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
    fun shouldValidateSetterPropertiesNotExplicitlySet() {

        val json = """{ "left": "123" }"""

        val violations = assertViolationsOnDeserialization(json, BeanWithCreatorParamAndProperty::class.java)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("right")
    }


    @JsonValidated
    class BeanWithStaticFactoryMethod(val left: String, val right: String) {

        companion object {
            @JvmStatic
            @JsonCreator
            fun create(
                @NotNull @JsonProperty("left") left: String,
                @NotNull @JsonProperty("right") right: String
            ) = BeanWithStaticFactoryMethod(left, right)
        }
    }


    @Test
    fun shouldValidateStaticFactoryMethod() {

        val json = """{ "left": "123" }"""

        val violations = assertViolationsOnDeserialization(json, BeanWithCreatorParamAndProperty::class.java)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNull>("right")
    }
}
