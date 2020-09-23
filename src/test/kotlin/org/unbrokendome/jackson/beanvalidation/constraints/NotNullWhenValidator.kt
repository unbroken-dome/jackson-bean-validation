package org.unbrokendome.jackson.beanvalidation.constraints

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import java.util.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class NotNullWhenValidator : ConstraintValidator<NotNullWhen, String?> {
    private var field: String = ""
    private var value: String = ""

    override fun initialize(notNullWhen: NotNullWhen) {
        field = notNullWhen.field
        value = notNullWhen.value
    }

    override fun isValid(data: String?, context: ConstraintValidatorContext): Boolean {
        val hibernateValidatorCtx = context.unwrap(HibernateConstraintValidatorContext::class.java)
        val constructorParams = hibernateValidatorCtx.getConstraintValidatorPayload(Map::class.java)
        val actualValue = constructorParams[field]
        return actualValue == null ||
                !Objects.equals(actualValue.toString(), value) ||
                data != null
    }
}
