@file:JvmName("KotlinPropertyUtils")
package org.unbrokendome.jackson.beanvalidation

import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import com.fasterxml.jackson.databind.introspect.AnnotatedField
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty


internal fun isNotNullProperty(prop: SettableBeanProperty): Boolean {
    when (prop.member) {
        is AnnotatedParameter -> {
            val parameter = prop.member as AnnotatedParameter
            val kotlinCallable: KFunction<*> =
                when (val methodOrConstructor = parameter.member) {
                    is Constructor<*> -> methodOrConstructor.kotlinFunction
                    is Method -> methodOrConstructor.kotlinFunction
                    else -> null
                } ?: return false
            return !kotlinCallable.valueParameters[prop.creatorIndex].type.isMarkedNullable
        }
        is AnnotatedField -> {
            val field = (prop.member as AnnotatedField).annotated
            return field.kotlinProperty?.returnType?.isMarkedNullable != true
        }
        is AnnotatedMethod -> {
            val property = (prop.member as AnnotatedMethod).annotated
                .findMemberPropertyForSetter()
            return property != null && !property.returnType.isMarkedNullable
        }
        else -> return false
    }
}


internal fun isKotlinLateinitVar(prop: SettableBeanProperty): Boolean =
    (prop.member as? AnnotatedMethod)?.let { annotatedMethod ->
        val property = annotatedMethod.member.findMemberPropertyForSetter()
        return property != null && property.isLateinit
    } ?: false


private fun Method.findMemberPropertyForSetter(): KProperty<*>? =
    declaringClass.kotlin.declaredMemberProperties
        .find { it is KMutableProperty<*> && it.setter.javaMethod == this }
