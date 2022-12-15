package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import java.time.LocalDate
import jakarta.validation.constraints.NotNull


class ValidInputTest : AbstractValidationTest() {

    @BeforeEach
    fun registerJavaTimeModule() {
        objectMapper.registerModule(JavaTimeModule())
    }


    @JsonValidated
    class TestBean1
    @JsonCreator constructor(
        @param:JsonProperty("date") val date: LocalDate
    )


    @Test
    fun `should report mismatched input as violation`() {

        val json = """{ "date": "20xx-11-15" }"""

        val violations = assertViolationsOnDeserialization(
            json, TestBean1::class.java
        )

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<JsonValidInput>("date")
    }


    @JsonValidated
    class TestBean2
    @JsonCreator constructor(@param:JsonProperty("date") val date: LocalDate) {

        @get:NotNull
        var otherValue: String? = null
    }


    @Test
    fun `should report mismatched input and validate setter properties`() {

        val json = """{ "date": "20xx-11-15", "otherValue": null }"""

        val violations = assertViolationsOnDeserialization(
            json, TestBean2::class.java
        )

        assertThat(violations).hasSize(2)
        assertThat(violations).hasViolation<JsonValidInput>("date")
        assertThat(violations).hasViolation<NotNull>("otherValue")
    }


    @JsonValidated
    class TestBean3
    @JsonCreator constructor(
        @param:JsonProperty("date") val date: LocalDate,
        @param:JsonProperty("otherValue") @param:NotNull val otherValue: String
    )


    @Test
    fun `should report mismatched input and validate creator params`() {

        val json = """{ "date": "20xx-11-15", "otherValue": null }"""

        val violations = assertViolationsOnDeserialization(
            json, TestBean3::class.java
        )

        assertThat(violations).hasSize(2)
        assertThat(violations).hasViolation<JsonValidInput>("date")
        assertThat(violations).hasViolation<NotNull>("otherValue")
    }
}
