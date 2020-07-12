package com.serverjars.updater;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Jacobtread
 * -
 * created on 7/11/20 at 11:23 PM
 */
public final class Agent {

    static void addToClassPath(final File jar) {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (!(loader instanceof URLClassLoader)) {
            throw new RuntimeException("System ClassLoader is not URLClassLoader");
        }
        try {
            final Method addURL = getAddMethod(loader);
            if (addURL == null) {
                System.err.println("Unable to find method to add the jar to System ClassLoader");
                System.exit(1);
            }
            addURL.setAccessible(true);
            addURL.invoke(loader, jar.toURI().toURL());
        } catch (final IllegalAccessException | InvocationTargetException | MalformedURLException e) {
            System.err.println("Unable to add the Jar to System ClassLoader");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Method getAddMethod(final Object o) {
        Class<?> clazz = o.getClass();
        Method m = null;
        while (m == null) {
            try {
                m = clazz.getDeclaredMethod("addURL", URL.class);
            } catch (final NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    return null;
                }
            }
        }
        return m;
    }


}
