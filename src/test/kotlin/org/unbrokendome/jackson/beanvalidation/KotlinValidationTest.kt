package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
        @param:NotNull
        val left: String,
        @param:NotNull
        val right: Int
    )

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
}
