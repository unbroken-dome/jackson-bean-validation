package org.unbrokendome.jackson.beanvalidation.assertions

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.support.expected
import assertk.assertions.support.show
import jakarta.validation.ConstraintViolation


inline fun <reified C : Any> Assert<ConstraintViolation<*>>.hasConstraintClass() =
    given { actual ->
        assertThat(actual.constraintDescriptor.annotation).isInstanceOf(C::class)
    }


inline fun <reified C : Any> Assert<Iterable<ConstraintViolation<*>>>.hasViolation(path: String) =
        given { actual ->
            val hasMatchingViolation = actual.asSequence()
                .filter { violation ->
                    violation.propertyPath.toString() == path &&
                            C::class.isInstance(violation.constraintDescriptor.annotation)
                }
                .any()

            if (!hasMatchingViolation) {
                expected("to contain a @${C::class.simpleName} violation at path \"$path\" but was: ${show(actual)}")
            }
        }
