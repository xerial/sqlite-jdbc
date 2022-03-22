package org.sqlite.util;

import java.lang.annotation.Annotation;

public class ExtentionEvaluator {

    public static boolean shouldEnable(Object enableWhenExtPresent) {
        System.out.printf(" in shouldEnable with obj %s%n", enableWhenExtPresent.getClass().getName());
        Class<?> cl = enableWhenExtPresent.getClass();
//        Annotation annotation = cl.getAnnotation();
        return true;
    }
}
