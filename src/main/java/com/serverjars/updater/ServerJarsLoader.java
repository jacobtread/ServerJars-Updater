package com.serverjars.updater;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public final class ServerJarsLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    public ServerJarsLoader(java.lang.ClassLoader parent) {
        super(new URL[0], parent);
    }

    public ServerJarsLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public static ServerJarsLoader findAncestor(java.lang.ClassLoader cl) {
        do {
            if (cl instanceof ServerJarsLoader)
                return (ServerJarsLoader) cl;
            cl = cl.getParent();
        } while (cl != null);

        return null;
    }

    void add(URL url) {
        addURL(url);
    }

    @SuppressWarnings("unused")
    private void appendToClassPathForInstrumentation(String jarfile) throws IOException {
        add(Paths.get(jarfile).toRealPath().toUri().toURL());
    }
}