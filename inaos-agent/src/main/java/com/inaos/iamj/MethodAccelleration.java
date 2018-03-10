package com.inaos.iamj;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.bytebuddy.matcher.ElementMatchers.*;

class MethodAccelleration {

    private static final MethodDescription.InDefinedShape TYPE, METHOD, PARAMETERS, DISPATCHERS, LIBRARIES;

    static {
        TypeDescription accelleration = new TypeDescription.ForLoadedType(Accelleration.class);
        TYPE = accelleration.getDeclaredMethods().filter(named("type")).getOnly();
        METHOD = accelleration.getDeclaredMethods().filter(named("method")).getOnly();
        PARAMETERS = accelleration.getDeclaredMethods().filter(named("parameters")).getOnly();
        DISPATCHERS = accelleration.getDeclaredMethods().filter(named("dispatchers")).getOnly();
        LIBRARIES = accelleration.getDeclaredMethods().filter(named("libraries")).getOnly();
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
                            if (typeDescription.getDeclaredAnnotations().isAnnotationPresent(Accelleration.class)) {
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

    private final Advice advice;

    private final Map<TypeDescription, byte[]> injection = new HashMap<TypeDescription, byte[]>();

    private final List<String> libraries;

    private final ClassLoader classLoader;

    private MethodAccelleration(TypeDescription typeDescription, ClassFileLocator classFileLocator, ClassLoader classLoader) throws IOException {
        this.typeDescription = typeDescription;
        advice = Advice.withCustomMapping()
                .bind(DevMode.class, false) // TODO: Resolve dev mode
                .to(new TypeDescription.ForLoadedType(EnterAdvice.class), typeDescription, classFileLocator);
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Accelleration.class);
        for (TypeDescription type : annotation.getValue(DISPATCHERS).resolve(TypeDescription[].class)) {
            injection.put(type, classFileLocator.locate(type.getName()).resolve());
        }
        libraries = Arrays.asList(annotation.getValue(LIBRARIES).resolve(String[].class));
        this.classLoader = classLoader;
    }

    public Advice getAdvice() {
        return advice;
    }

    public Map<TypeDescription, byte[]> injection() {
        return injection;
    }

    public List<String> libraries() {
        return libraries;
    }

    public ElementMatcher<TypeDescription> type() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Accelleration.class);
        return is(annotation.getValue(TYPE).resolve(TypeDescription.class));
    }

    public ElementMatcher<MethodDescription> method() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Accelleration.class);
        return named(annotation.getValue(METHOD).resolve(String.class)).and(takesArguments(annotation.getValue(PARAMETERS).resolve(TypeDescription[].class)));
    }

    public InputStream resourceAsStream(String name) {
        return classLoader.getResourceAsStream(name);
    }
}
