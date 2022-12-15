package org.unbrokendome.jackson.beanvalidation

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation


abstract class AbstractValidationTest {

    private val validatorFactory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()

    protected val beanValidationModule: BeanValidationModule = BeanValidationModule(validatorFactory)

    protected val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(beanValidationModule)


    protected fun assertViolationsOnDeserialization(
        json: String,
        targetType: Class<out Any>
    ): Set<ConstraintViolation<*>> {
        return assertViolations {
            objectMapper.readValue(json, targetType)
        }
    }


    protected inline fun <reified T : Any> assertViolationsOnDeserialization(json: String): Set<ConstraintViolation<*>> =
        assertViolationsOnDeserialization(json, T::class.java)


    protected fun assertNoViolationsOnDeserialization(
        json: String,
        targetType: Class<out Any>
    ) {
        assertNoViolations {
            objectMapper.readValue(json, targetType)
        }
    }


    protected inline fun <reified T : Any> assertNoViolationsOnDeserialization(json: String) {
        assertNoViolationsOnDeserialization(json, T::class.java)
    }


    protected fun assertViolations(block: () -> Unit): Set<ConstraintViolation<*>> {
        val exception = assertThrows(ConstraintViolationException::class.java, block)
        return exception.constraintViolations
    }


    protected fun assertNoViolations(block: () -> Unit) {
        assertDoesNotThrow(block)
    }
}
