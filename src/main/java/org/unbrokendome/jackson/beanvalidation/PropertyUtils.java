package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;


final class PropertyUtils {

    private PropertyUtils() { }


    static String getPropertyNameFromMember(AnnotatedMember member) {
        return getPropertyNameFromMember(member.getMember());
    }


    static String getPropertyNameFromMember(Member member) {
        if (member instanceof Method) {
            return getPropertyNameFromAccessorMethod((Method) member);

        } else if (member instanceof Field) {
            return member.getName();

        } else {
            throw new IllegalArgumentException("Member must be a field or an accessor method, but was: " + member);
        }
    }


    static String getPropertyNameFromAccessorMethod(Method accessor) {

        String accessorName = accessor.getName();

        if (accessorName.length() > 3 &&
                (accessorName.startsWith("get") || accessorName.startsWith("set")) &&
                Character.isUpperCase(accessorName.charAt(3))) {
            return StringUtils.decapitalizeSubstring(accessorName, 3);

        } else if (accessorName.length() > 2 && accessorName.startsWith("is") &&
                Character.isUpperCase(accessorName.charAt(2)) &&
                (accessor.getReturnType() == Boolean.TYPE || accessor.getReturnType() == Boolean.class)) {
            return StringUtils.decapitalizeSubstring(accessorName, 2);

        } else {
            throw new IllegalArgumentException("Not a property accessor method: " + accessor);
        }
    }


    static Object getProperty(Object bean, String propertyName) {

        Class<?> beanClass = bean.getClass();
        String capitalizedPropertyName = StringUtils.capitalize(propertyName);

        Method accessorMethod;
        try {
            String accessorName = "get" + capitalizedPropertyName;
            accessorMethod = beanClass.getMethod(accessorName);

        } catch (NoSuchMethodException ex) {

            String accessorName = "is" + capitalizedPropertyName;
            try {
                accessorMethod = beanClass.getMethod(accessorName);
                if (accessorMethod.getReturnType() != Boolean.TYPE &&
                        accessorMethod.getReturnType() != Boolean.class) {
                    throw new IllegalArgumentException("No property getter found for property " + propertyName +
                            " on class " + beanClass + ". A method " + accessorName + "() exists but the return" +
                            " type is not boolean or Boolean");
                }

            } catch (NoSuchMethodException ex2) {
                throw new IllegalArgumentException("No property getter found for property " + propertyName +
                        accessorName + " on class " + beanClass, ex);
            }
        }

        try {
            return accessorMethod.invoke(bean);

        } catch (ReflectiveOperationException ex) {
            throw new UncheckedReflectiveOperationException("Exception trying to invoke property " +
                    "accessor " + accessorMethod, ex);
        }
    }
}
