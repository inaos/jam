package com.inaos.iamj.agent;

import com.inaos.iamj.api.Acceleration;
import com.inaos.iamj.api.DevMode;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.bytebuddy.matcher.ElementMatchers.*;

class MethodAccelleration {

    private static final MethodDescription.InDefinedShape TYPE, METHOD, PARAMETERS, LIBRARIES, SIMPLE_ENTRY, DISPATCHER, BINARY, SYSTEM_LOAD, INLINE;

    static {
        TypeDescription accelleration = new TypeDescription.ForLoadedType(Acceleration.class);
        TYPE = accelleration.getDeclaredMethods().filter(named("type")).getOnly();
        METHOD = accelleration.getDeclaredMethods().filter(named("method")).getOnly();
        PARAMETERS = accelleration.getDeclaredMethods().filter(named("parameters")).getOnly();
        LIBRARIES = accelleration.getDeclaredMethods().filter(named("libraries")).getOnly();
        SIMPLE_ENTRY = accelleration.getDeclaredMethods().filter(named("simpleEntry")).getOnly();
        INLINE = accelleration.getDeclaredMethods().filter(named("inline")).getOnly();
        TypeDescription library = new TypeDescription.ForLoadedType(Acceleration.Library.class);
        DISPATCHER = library.getDeclaredMethods().filter(named("dispatcher")).getOnly();
        BINARY = library.getDeclaredMethods().filter(named("binary")).getOnly();
        TypeDescription system = new TypeDescription.ForLoadedType(System.class);
        SYSTEM_LOAD = system.getDeclaredMethods().filter(named("load")).getOnly();
    }

    static List<MethodAccelleration> findAll(URL url) {
        ClassLoader classLoader = new URLClassLoader(new URL[]{url});
        Set<ClassLoader> classLoaders = Collections.newSetFromMap(new IdentityHashMap<ClassLoader, Boolean>());
        classLoaders.add(classLoader);
        classLoaders.add(Acceleration.class.getClassLoader());
        classLoaders.add(Advice.class.getClassLoader());
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
        for (ClassLoader loader : classLoaders) {
            classFileLocators.add(ClassFileLocator.ForClassLoader.of(loader));
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);

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

    private MethodAccelleration(TypeDescription typeDescription, ClassFileLocator classFileLocator, ClassLoader classLoader) {
        this.typeDescription = typeDescription;
        this.classFileLocator = classFileLocator;
        this.classLoader = classLoader;
    }

    Advice advice(boolean devMode) {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        Advice.WithCustomMapping advice = Advice.withCustomMapping().bind(DevMode.class, devMode);
        if (annotation.getValue(SIMPLE_ENTRY).resolve(Boolean.class)) {
            return advice.to(new TypeDescription.ForLoadedType(TrivialEnterAdvice.class), typeDescription, classFileLocator);
        } else {
            return advice.to(typeDescription, classFileLocator);
        }
    }

    ElementMatcher<TypeDescription> type() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        return is(annotation.getValue(TYPE).resolve(TypeDescription.class));
    }

    ElementMatcher<MethodDescription> method() {
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        return named(annotation.getValue(METHOD).resolve(String.class)).and(takesArguments(annotation.getValue(PARAMETERS).resolve(TypeDescription[].class)));
    }

    Binaries binaries(ByteBuddy byteBuddy, String folder, String prefix, String extension, ClassLoader userLoader) {
        List<DynamicType.Unloaded<?>> types = new ArrayList<DynamicType.Unloaded<?>>();
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        List<Runnable> destructions = new ArrayList<Runnable>();
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
            TypeDescription dispatcher = library.getValue(DISPATCHER).resolve(TypeDescription.class);
            Implementation initialization = StubMethod.INSTANCE;
            for (MethodDescription initMethod : dispatcher.getDeclaredMethods().filter(isAnnotatedWith(Acceleration.Library.Init.class))) {
                if (!initMethod.isStatic() || !initMethod.getParameters().isEmpty() || !initMethod.getReturnType().represents(void.class)) {
                    throw new IllegalStateException("Stateful initializer method: " + initMethod);
                }
                initialization = MethodCall.invoke(initMethod).andThen(initialization);
            }
            List<String> destructionMethods = new ArrayList<String>();
            for (MethodDescription destroyMethod : dispatcher.getDeclaredMethods().filter(isAnnotatedWith(Acceleration.Library.Destroy.class))) {
                if (!destroyMethod.isStatic() || !destroyMethod.getParameters().isEmpty() || !destroyMethod.getReturnType().represents(void.class)) {
                    throw new IllegalStateException("Stateful destruction method: " + destroyMethod);
                }
                destructionMethods.add(destroyMethod.getName());
            }
            if (!destructionMethods.isEmpty()) {
                destructions.add(new Destruction(userLoader, dispatcher.getName(), destructionMethods));
            }
            types.add(byteBuddy.redefine(dispatcher, classFileLocator)
                    .invokable(isTypeInitializer())
                    .intercept(MethodCall.invoke(SYSTEM_LOAD).with(file.getAbsolutePath()).andThen(initialization))
                    .make());
        }
        return new Binaries(types, destructions);
    }

    Map<TypeDescription, byte[]> inlined() {
        Map<TypeDescription, byte[]> inlined = new HashMap<TypeDescription, byte[]>();
        AnnotationDescription annotation = typeDescription.getDeclaredAnnotations().ofType(Acceleration.class);
        try {
            for (TypeDescription inline : annotation.getValue(INLINE).resolve(TypeDescription[].class)) {
                inlined.put(inline, classFileLocator.locate(inline.getName()).resolve());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return inlined;
    }

    static class Binaries {

        final List<DynamicType.Unloaded<?>> types;

        final List<Runnable> destructions;

        private Binaries(List<DynamicType.Unloaded<?>> types, List<Runnable> destructions) {
            this.types = types;
            this.destructions = destructions;
        }
    }

    private static class Destruction implements Runnable {

        private final ClassLoader classLoader;

        private final String type;

        private final List<String> destroyMethods;

        private Destruction(ClassLoader classLoader, String type, List<String> destroyMethods) {
            this.classLoader = classLoader;
            this.type = type;
            this.destroyMethods = destroyMethods;
        }

        @Override
        public void run() {
            try {
                Class<?> dispatcher = Class.forName(type, false, classLoader);
                for (String destroyMethod : destroyMethods) {
                    Method method = dispatcher.getDeclaredMethod(destroyMethod);
                    method.setAccessible(true);
                    method.invoke(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
