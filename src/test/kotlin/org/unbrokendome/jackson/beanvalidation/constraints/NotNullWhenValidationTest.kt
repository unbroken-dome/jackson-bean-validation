package org.unbrokendome.jackson.beanvalidation.constraints

import assertk.assertThat
import assertk.assertions.hasSize
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.unbrokendome.jackson.beanvalidation.AbstractValidationTest
import org.unbrokendome.jackson.beanvalidation.JsonValidated
import org.unbrokendome.jackson.beanvalidation.assertions.hasViolation

class NotNullWhenValidationTest : AbstractValidationTest(HibernateConstructorValidatorFactory()) {

    @JsonValidated
    class BeanWithCrossValidation
    @JsonCreator constructor(
            @param:JsonProperty("hasDetails")
            val hasDetails: Boolean,
            @param:JsonProperty("details")
            @NotNullWhen(field = "hasDetails", value = "true")
            val details: String?
    )

    @Test
    fun `should not report violation when precondition is not matched`() {

        val json = """{ "hasDetails": false, "details": "" }"""

        assertNoViolationsOnDeserialization<BeanWithCrossValidation>(json)
    }


    @Test
    fun `should not report violation when precondition is matched and value is valid`() {

        val json = """{ "hasDetails": false, "details": "secure" }"""

        assertNoViolationsOnDeserialization<BeanWithCrossValidation>(json)
    }

    @Test
    fun `should report violation when precondition is matched and value is null`() {

        val json = """{ "hasDetails": true }"""

        val violations = assertViolationsOnDeserialization<BeanWithCrossValidation>(json)

        assertThat(violations).hasSize(1)
        assertThat(violations).hasViolation<NotNullWhen>("details")
    }

    @Test
    fun `should not report violation when precondition is matched and value is empty`() {

        val json = """{ "hasDetails": true, "details": "" }"""

        assertNoViolationsOnDeserialization<BeanWithCrossValidation>(json)
    }
}
