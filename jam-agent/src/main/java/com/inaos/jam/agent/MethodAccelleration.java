/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import com.inaos.jam.api.Acceleration;
import com.inaos.jam.boot.JamDestructor;
import com.inaos.jam.tool.Platform;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
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

    private static final MethodDescription.InDefinedShape TYPE,
            METHOD,
            PARAMETERS,
            LIBRARIES,
            CHECKSUM,
            APPLICATION,
            SIMPLE_ENTRY,
            DISPATCHER,
            BINARY,
            SYSTEM_LOAD,
            SYSTEM_LOAD_LIBRARY,
            INLINE,
            EXPECTED_NAMES,
            CAPTURE,
            CAPTURE_TYPE,
            CAPTURE_FIELDS,
            REGISTER_DESTRUCTOR;

    static {
        TypeDescription accelleration = new TypeDescription.ForLoadedType(Acceleration.class);
        TYPE = accelleration.getDeclaredMethods().filter(named("type")).getOnly();
        METHOD = accelleration.getDeclaredMethods().filter(named("method")).getOnly();
        PARAMETERS = accelleration.getDeclaredMethods().filter(named("parameters")).getOnly();
        LIBRARIES = accelleration.getDeclaredMethods().filter(named("libraries")).getOnly();
        CHECKSUM = accelleration.getDeclaredMethods().filter(named("checksum")).getOnly();
        APPLICATION = accelleration.getDeclaredMethods().filter(named("application")).getOnly();
        SIMPLE_ENTRY = accelleration.getDeclaredMethods().filter(named("simpleEntry")).getOnly();
        INLINE = accelleration.getDeclaredMethods().filter(named("inline")).getOnly();
        EXPECTED_NAMES = accelleration.getDeclaredMethods().filter(named("expectedNames")).getOnly();
        CAPTURE = accelleration.getDeclaredMethods().filter(named("capture")).getOnly();
        TypeDescription library = new TypeDescription.ForLoadedType(Acceleration.Library.class);
        DISPATCHER = library.getDeclaredMethods().filter(named("dispatcher")).getOnly();
        BINARY = library.getDeclaredMethods().filter(named("binary")).getOnly();
        TypeDescription system = new TypeDescription.ForLoadedType(System.class);
        SYSTEM_LOAD = system.getDeclaredMethods().filter(named("load")).getOnly();
        SYSTEM_LOAD_LIBRARY = system.getDeclaredMethods().filter(named("loadLibary")).getOnly();
        TypeDescription capture = new TypeDescription.ForLoadedType(Acceleration.Capture.class);
        CAPTURE_TYPE = capture.getDeclaredMethods().filter(named("type")).getOnly();
        CAPTURE_FIELDS = capture.getDeclaredMethods().filter(named("fields")).getOnly();
        TypeDescription desctructor = new TypeDescription.ForLoadedType(JamDestructor.class);
        REGISTER_DESTRUCTOR = desctructor.getDeclaredMethods().filter(named("register")).getOnly();
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

        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);

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
                                if (typeDescription.getDeclaredMethods()
                                        .filter(isAnnotatedWith(Advice.OnMethodEnter.class).or(isAnnotatedWith(Advice.OnMethodExit.class)))
                                        .isEmpty()) {
                                    throw new IllegalStateException("Acceleration is not an advice class: " + typeDescription);
                                }
                                accellerations.add(new MethodAccelleration(typeDescription.getName(),
                                        typeDescription.getDeclaredAnnotations().ofType(Acceleration.class),
                                        classFileLocator,
                                        classLoader));
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

    private final String target;

    private final AnnotationDescription.Loadable<Acceleration> annotation;

    private final ClassFileLocator classFileLocator;

    private final ClassLoader classLoader;

    private MethodAccelleration(String target,
                                AnnotationDescription.Loadable<Acceleration> annotation,
                                ClassFileLocator classFileLocator,
                                ClassLoader classLoader) {
        this.target = target;
        this.annotation = annotation;
        this.classFileLocator = classFileLocator;
        this.classLoader = classLoader;
    }

    String type() {
        return annotation.getValue(TYPE).resolve(TypeDescription.class).getName();
    }

    AgentBuilder.RawMatcher typeMatcher(boolean noExpectedName) {
        AgentBuilder.RawMatcher matcher = new AgentBuilder.RawMatcher.ForElementMatchers(named(annotation.getValue(TYPE)
                .resolve(TypeDescription.class)
                .getName()));
        String[] expectedNames = annotation.getValue(EXPECTED_NAMES).resolve(String[].class);
        if (noExpectedName || expectedNames.length == 0) {
            return matcher;
        } else {
            return new CodeSourceMatcher(matcher, Arrays.asList(expectedNames));
        }
    }

    boolean isTrivialEnter() {
        return annotation.getValue(SIMPLE_ENTRY).resolve(Boolean.class);
    }

    boolean isActive(boolean devMode) {
        String value = annotation.getValue(APPLICATION).resolve(EnumerationDescription.class).getValue();
        if (value.equals(Acceleration.Application.DEVELOPMENT.name())) {
            return devMode;
        } else if (value.equals(Acceleration.Application.PRODUCTION.name())) {
            return !devMode;
        } else {
            return true;
        }
    }

    ElementMatcher<MethodDescription> method() {
        TypeDescription[] arguments = annotation.getValue(PARAMETERS).resolve(TypeDescription[].class);
        ElementMatcher.Junction<MethodDescription> types = any();
        int index = 0;
        for (TypeDescription argument : arguments) {
            types = types.and(takesArgument(index++, named(argument.getActualName())));
        }
        return named(annotation.getValue(METHOD).resolve(String.class))
                .and(takesArguments(arguments.length))
                .and(types)
                .and(not(isBridge()));
    }

    String target() {
        return target;
    }

    ClassFileLocator classFileLocator() {
        return classFileLocator;
    }

    LiveBinaries liveBinaries(ByteBuddy byteBuddy, Platform platform, ClassLoader userLoader) {
        List<DynamicType.Unloaded<?>> types = new ArrayList<DynamicType.Unloaded<?>>();
        List<Runnable> destructions = new ArrayList<Runnable>();
        for (AnnotationDescription library : annotation.getValue(LIBRARIES).resolve(AnnotationDescription[].class)) {
            String resource = platform.folder + "/" + platform.prefix + library.getValue(BINARY).resolve(String.class);
            InputStream in = classLoader.getResourceAsStream(resource + "." + platform.extension);
            File file;
            try {
                file = File.createTempFile(resource, "." + platform.extension);
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
            ClassFileLocator compoundLocator = new ClassFileLocator.Compound(classFileLocator, ClassFileLocator.ForClassLoader.of(userLoader));
            dispatcher = TypePool.Default.WithLazyResolution.of(compoundLocator).describe(dispatcher.getName()).resolve();

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
            types.add(byteBuddy.redefine(dispatcher, compoundLocator)
                    .invokable(isTypeInitializer())
                    .intercept(MethodCall.invoke(SYSTEM_LOAD).with(file.getAbsolutePath()).andThen(initialization))
                    .make());
        }
        return new LiveBinaries(types, destructions);
    }

    StaleBinaries staleBinaries(ByteBuddy byteBuddy, Platform platform, ClassFileLocator classFileLocator) {
        Map<String, byte[]> binaries = new HashMap<String, byte[]>();
        List<DynamicType> types = new ArrayList<DynamicType>();
        for (AnnotationDescription library : annotation.getValue(LIBRARIES).resolve(AnnotationDescription[].class)) {
            String resource = platform.folder + "/" + platform.prefix + library.getValue(BINARY).resolve(String.class);
            TypeDescription dispatcher = library.getValue(DISPATCHER).resolve(TypeDescription.class);
            ClassFileLocator compoundLocator = new ClassFileLocator.Compound(this.classFileLocator, classFileLocator);
            dispatcher = TypePool.Default.WithLazyResolution.of(compoundLocator).describe(dispatcher.getName()).resolve();

            Implementation initialization = StubMethod.INSTANCE;
            for (MethodDescription initMethod : dispatcher.getDeclaredMethods().filter(isAnnotatedWith(Acceleration.Library.Init.class))) {
                if (!initMethod.isStatic() || !initMethod.getParameters().isEmpty() || !initMethod.getReturnType().represents(void.class)) {
                    throw new IllegalStateException("Stateful initializer method: " + initMethod);
                }
                initialization = MethodCall.invoke(initMethod).andThen(initialization);
            }
            for (MethodDescription destroyMethod : dispatcher.getDeclaredMethods().filter(isAnnotatedWith(Acceleration.Library.Destroy.class))) {
                if (!destroyMethod.isStatic() || !destroyMethod.getParameters().isEmpty() || !destroyMethod.getReturnType().represents(void.class)) {
                    throw new IllegalStateException("Stateful destruction method: " + destroyMethod);
                }
                initialization = MethodCall.invoke(REGISTER_DESTRUCTOR).with(dispatcher, destroyMethod.getName()).andThen(initialization);
            }
            types.add(byteBuddy.redefine(dispatcher, compoundLocator)
                    .invokable(isTypeInitializer())
                    .intercept(MethodCall.invoke(SYSTEM_LOAD_LIBRARY).with(resource).andThen(initialization))
                    .make());
            InputStream in = classLoader.getResourceAsStream(resource + "." + platform.extension);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
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
                binaries.put(resource + "." + platform.extension, out.toByteArray());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new StaleBinaries(types, binaries);
    }

    List<Capture> captures() {
        List<Capture> captures = new ArrayList<Capture>();
        for (AnnotationDescription library : annotation.getValue(CAPTURE).resolve(AnnotationDescription[].class)) {
            String name = library.getValue(CAPTURE_TYPE).resolve(TypeDescription.class).getName();
            String[] fields = library.getValue(CAPTURE_FIELDS).resolve(String[].class);
            captures.add(new Capture(name, fields));
        }
        return captures;
    }

    Map<TypeDescription, byte[]> inlined() {
        Map<TypeDescription, byte[]> inlined = new HashMap<TypeDescription, byte[]>();
        try {
            for (TypeDescription inline : annotation.getValue(INLINE).resolve(TypeDescription[].class)) {
                inlined.put(inline, classFileLocator.locate(inline.getName()).resolve());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return inlined;
    }

    boolean checksum(final ClassFileLocator classFileLocator, final boolean debug) {
        List<String> checksums = Arrays.asList(annotation.getValue(CHECKSUM).resolve(String[].class));
        ClassFileLocator.Resolution resolution;
        try {
            resolution = classFileLocator.locate(annotation.getValue(TYPE).resolve(TypeDescription.class).getName());
        } catch (IOException e) {
            if (debug) {
                System.out.println("I/O error during checksum computation: " + e.getMessage());
            }
            resolution = null;
        }
        if (resolution == null || !resolution.isResolved()) {
            if (debug) {
                System.out.println("Could not locate class file for computing checksum for: " + this);
            }
            return checksums.isEmpty();
        }
        final String[] computed = new String[1];
        final StringBuilder sb = new StringBuilder().append("(");
        for (TypeDescription typeDescription : annotation.getValue(PARAMETERS).resolve(TypeDescription[].class)) {
            sb.append(typeDescription.getDescriptor());
        }
        sb.append(")");
        new ClassReader(resolution.resolve()).accept(new ClassVisitor(Opcodes.ASM6) {
            private boolean methodFound;

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_BRIDGE) == 0
                        && name.equals(annotation.getValue(METHOD).resolve(String.class))
                        && desc.startsWith(sb.toString())) {
                    methodFound = true;
                    return new CheckSumVisitor() {
                        @Override
                        void onChecksum(String checksum) {
                            computed[0] = checksum;
                        }
                    };
                }
                return null;
            }

            @Override
            public void visitEnd() {
                if (debug && !methodFound) {
                    System.out.println("Could not locate method " + annotation.getValue(METHOD).resolve(String.class)
                            + sb.toString()
                            + " in " + annotation.getValue(TYPE).resolve(TypeDescription.class).getName()
                            + " of " + classLoader);
                }
            }
        }, ClassReader.SKIP_DEBUG);
        if (computed[0] == null) {
            if (debug) {
                System.out.println("Could not compute checksum for " + this + " (MD5 available: " + CheckSumVisitor.isMd5Available() + ")");
            }
            return !CheckSumVisitor.isMd5Available();
        } else {
            if (checksums.contains(computed[0])) {
                return true;
            } else if (debug) {
                System.out.println("Checksum for " + this + " not equal: " + computed[0]);
            }
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(target).append(" @ (");
        sb.append("type=").append(annotation.getValue(TYPE).resolve(TypeDescription.class).getName());
        sb.append(", method=").append(annotation.getValue(METHOD).resolve(String.class));
        sb.append(", parameters=[");
        boolean first = true;
        for (TypeDescription parameter : annotation.getValue(PARAMETERS).resolve(TypeDescription[].class)) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(parameter.getName());
        }
        sb.append("]");
        return sb.append(")").toString();
    }

    static class LiveBinaries {

        final List<DynamicType.Unloaded<?>> types;

        final List<Runnable> destructions;

        private LiveBinaries(List<DynamicType.Unloaded<?>> types, List<Runnable> destructions) {
            this.types = types;
            this.destructions = destructions;
        }
    }

    static class StaleBinaries {

        final List<DynamicType> types;

        final Map<String, byte[]> binaries;

        private StaleBinaries(List<DynamicType> types, Map<String, byte[]> binaries) {
            this.types = types;
            this.binaries = binaries;
        }
    }

    static class Capture {

        private final String name;

        private final String[] field;

        private Capture(String name, String[] field) {
            this.name = name;
            this.field = field;
        }

        String getName() {
            return name;
        }

        String[] getField() {
            return field;
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
