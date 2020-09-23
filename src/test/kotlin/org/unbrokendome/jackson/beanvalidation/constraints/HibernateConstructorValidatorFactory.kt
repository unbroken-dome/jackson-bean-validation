package org.unbrokendome.jackson.beanvalidation.constraints

import org.hibernate.validator.HibernateValidatorFactory
import org.unbrokendome.jackson.beanvalidation.ConstructorValidatorFactory
import java.lang.reflect.Constructor
import javax.validation.Validator
import javax.validation.ValidatorFactory

class HibernateConstructorValidatorFactory : ConstructorValidatorFactory {
    override fun getValidator(validatorFactory: ValidatorFactory, constructor: Constructor<Any>, parameterValues: Array<out Any>): Validator {
        val declaredFields = constructor.declaringClass.declaredFields
        val payload: Map<String, Any> = parameterValues
            .mapIndexed { index, value -> declaredFields[index].name to value}
            .toMap()

        return validatorFactory.unwrap(HibernateValidatorFactory::class.java)
            .usingContext().constraintValidatorPayload(payload).validator
    }
}
