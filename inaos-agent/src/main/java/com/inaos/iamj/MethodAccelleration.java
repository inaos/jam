package com.inaos.iamj;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.bytebuddy.matcher.ElementMatchers.*;

class MethodAccelleration {

    private static final MethodDescription.InDefinedShape TYPE, METHOD, PARAMETERS, LIBRARIES, DISPATCHER, BINARY, SYSTEM_LOAD;

    static {
        TypeDescription accelleration = new TypeDescription.ForLoadedType(Acceleration.class);
        TYPE = accelleration.getDeclaredMethods().filter(named("type")).getOnly();
        METHOD = accelleration.getDeclaredMethods().filter(named("method")).getOnly();
        PARAMETERS = accelleration.getDeclaredMethods().filter(named("parameters")).getOnly();
        LIBRARIES = accelleration.getDeclaredMethods().filter(named("libraries")).getOnly();
        TypeDescription library = new TypeDescription.ForLoadedType(Acceleration.Library.class);
        DISPATCHER = library.getDeclaredMethods().filter(named("dispatcher")).getOnly();
        BINARY = library.getDeclaredMethods().filter(named("binary")).getOnly();
        TypeDescription system = new TypeDescription.ForLoadedType(System.class);
        SYSTEM_LOAD = system.getDeclaredMethods().filter(named("load")).getOnly();
    }

    static List<MethodAccelleration> findAll(URL url) {
        ClassLoader classLoader = new URLClassLoader(new URL[]{url});
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.of(classLoader);
        TypePool typePool = TypePool.Default.of(classFileLocator);

        List<MethodAccelleration> accellerations = new ArrayList<MethodAccelleration>();

        try {
            ZipInputStream zip = new ZipInputStream(new BufferedInputStream(url.openStream()));
            try {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        String classFile = entry.getName().replace('/', '.');
                        TypePool.Resolution resolution = typePool.describe(classFile.substring(0, classFile.length() - ".class".length()));
                        if (resolution.isResolved()) {
                            TypeDescription typeDescription = resolution.resolve();
                            if (typeDescription.getDeclaredAnnotations().isAnnotationPresent(Acceleration.class)) {
                                accellerations.add(new MethodAccelleration(typeDescription, classFileLocator, classLoader));
                            }
                        } else {
                            System.out.println("Could not resolve: " + classFile);
                        }
                    }
                }
            } finally {
                zip.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return accellerations;
    }

    private final TypeDescription typeDescription;

    private final ClassFileLocator classFileLocator;

    private final ClassLoader classLoader;

    private MethodAccelleration(TypeDescription typeDescription, ClassFileLocator classFileLocator, ClassLoader classLoader) throws IOException {
        this.typeDescription = typeDescription;
        this.classFileLocator = classFileLocator;
        this.classLoader = classLoader;
    }

    Advice advice(boolean devMode) {
        return Advice.withCustomMapping()
                .bind(DevMode.class, devMode)
                .to(new TypeDescription.ForLoadedType(EnterAdvice.class), typeDescription, classFileLocator);
    }

    ElementMatcher<TypeDescription> type() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        return is(annotation.getValue(TYPE).resolve(TypeDescription.class));
    }

    ElementMatcher<MethodDescription> method() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        return named(annotation.getValue(METHOD).resolve(String.class)).and(takesArguments(annotation.getValue(PARAMETERS).resolve(TypeDescription[].class)));
    }

    List<DynamicType.Unloaded<?>> binaries(ByteBuddy byteBuddy, String folder, String prefix, String extension) {
        List<DynamicType.Unloaded<?>> types = new ArrayList<DynamicType.Unloaded<?>>();
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        for (AnnotationDescription library : annotation.getValue(LIBRARIES).resolve(AnnotationDescription[].class)) {
            String resource = folder + "/" + prefix + library.getValue(BINARY).resolve(String.class);
            InputStream in = classLoader.getResourceAsStream(resource + "." + extension);
            File file;
            try {
                file = File.createTempFile(resource, "." + extension);
                OutputStream out = new FileOutputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    out.close();
                }
                in.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            types.add(byteBuddy.redefine(library.getValue(DISPATCHER).resolve(TypeDescription.class), classFileLocator)
                    .invokable(isTypeInitializer())
                    .intercept(MethodCall.invoke(SYSTEM_LOAD).with(file.getAbsolutePath()))
                    .make());
        }
        return types;
    }
}
