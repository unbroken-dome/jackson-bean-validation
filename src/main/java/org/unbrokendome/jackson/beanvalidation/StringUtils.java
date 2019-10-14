package org.unbrokendome.jackson.beanvalidation;


final class StringUtils {

    private StringUtils() {
    }


    static String decapitalizeSubstring(String s, int startIndex) {
        int length = s.length();
        if (startIndex > length) {
            throw new StringIndexOutOfBoundsException("String index out of range: " + startIndex);
        } else if (startIndex == length) {
            return "";
        }
        return Character.toLowerCase(s.charAt(startIndex)) + s.substring(startIndex + 1);
    }


    public static String decapitalize(String s) {
        return decapitalizeSubstring(s, 0);
    }


    static String capitalizeSubstring(String s, int startIndex) {
        int length = s.length();
        if (startIndex > length) {
            throw new StringIndexOutOfBoundsException("String index out of range: " + startIndex);
        } else if (startIndex == length) {
            return "";
        }
        return Character.toUpperCase(s.charAt(startIndex)) + s.substring(startIndex + 1);
    }

    static String capitalize(String s) {
        return capitalizeSubstring(s, 0);
    }
}
