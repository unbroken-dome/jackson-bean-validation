package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import javax.validation.constraints.NotNull


class KotlinValidationTest : AbstractValidationTest() {

    @BeforeEach
    fun registerKotlinModule() {
        objectMapper.registerModule(KotlinModule())
        objectMapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
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
}
