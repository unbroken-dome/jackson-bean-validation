package org.unbrokendome.jackson.beanvalidation

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import jakarta.validation.constraints.AssertTrue


class CrossValidationTest : AbstractValidationTest() {

    @JsonValidated
    class BeanWithCrossValidation
    @JsonCreator constructor(
        @param:JsonProperty("lower") val lower: Int,
        @param:JsonProperty("upper") val upper: Int
    ) {
        @AssertTrue
        fun isLowerBelowUpper(): Boolean = lower < upper
    }


    @Test
    fun `should not evaluate cross validation by default`() {

        val json = """{ "lower": 3, "upper": 2 }"""

        assertNoViolationsOnDeserialization<BeanWithCrossValidation>(json)
    }


    @Test
    fun `should report violation on cross validation if feature is enabled`() {

        beanValidationModule.enable(BeanValidationFeature.VALIDATE_BEAN_AFTER_CONSTRUCTION)

        val json = """{ "lower": 3, "upper": 2 }"""

        val violations = assertViolationsOnDeserialization<BeanWithCrossValidation>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<AssertTrue>("lowerBelowUpper")
    }
}
