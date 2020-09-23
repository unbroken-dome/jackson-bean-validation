package org.unbrokendome.jackson.beanvalidation.constraints

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention
@MustBeDocumented
@Constraint(validatedBy = [NotNullWhenValidator::class])
annotation class NotNullWhen(
    val field: String,
    val value: String,
    val message: String = "{javax.validation.constraints.NotNull.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

