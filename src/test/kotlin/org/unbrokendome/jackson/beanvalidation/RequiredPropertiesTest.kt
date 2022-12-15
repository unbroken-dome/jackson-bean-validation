package org.unbrokendome.jackson.beanvalidation

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation
import jakarta.validation.constraints.NotNull


class RequiredPropertiesTest : AbstractValidationTest() {

    @JsonValidated
    class BeanWithRequiredCreatorParams
    @JsonCreator constructor(
        @param:[JsonProperty("first", required = true)] val first: String?,
        @param:[JsonProperty("second") JsonRequired] val second: String?)


    @Test
    fun `Should report JsonRequired violation on missing required creator param`() {

        val json = """{ }"""

        val violations = assertViolationsOnDeserialization<BeanWithRequiredCreatorParams>(json)

        assertThat(violations).all {
            hasSize(2)
            hasViolation<JsonRequired>("first")
            hasViolation<JsonRequired>("second")
        }
    }


    @Test
    fun `Should not report JsonRequired violation on null required creator param`() {

        val json = """{ "first": null, "second": null }"""

        assertNoViolationsOnDeserialization<BeanWithRequiredCreatorParams>(json)
    }


    @JsonValidated
    class BeanWithRequiredNotNullCreatorParams
    @JsonCreator constructor(
        @param:[JsonProperty("first", required = true) NotNull] val first: String?,
        @param:[JsonProperty("second") JsonRequired NotNull] val second: String?)


    @Test
    fun `Should report NotNull violation on null required creator param`() {

        val json = """{ "first": null, "second": null }"""

        val violations = assertViolationsOnDeserialization<BeanWithRequiredNotNullCreatorParams>(json)

        assertThat(violations).all {
            hasSize(2)
            hasViolation<NotNull>("first")
            hasViolation<NotNull>("second")
        }
    }
}
