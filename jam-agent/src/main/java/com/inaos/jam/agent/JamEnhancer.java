package com.inaos.jam.agent;

import com.inaos.jam.api.DevMode;
import com.inaos.jam.boot.JamAgentDispatcher;
import com.inaos.jam.boot.JamObjectCapture;
import com.inaos.jam.tool.Platform;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.pool.TypePool;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class JamEnhancer {

    private static final long MAX_OBSERVATION_COUNT_FOR_FILES = 10000;

    private static final long MAX_OBSERVATION_BYTES_FOR_FILES = 1024 * 1024; // 1 MB

    private final URL url;

    private final boolean isDevMode, isDebugMode, shouldIgnoreChecksum;

    private final Set<String> filtered;

    private final File sample;

    public JamEnhancer(URL url, boolean isDevMode, boolean isDebugMode, boolean shouldIgnoreChecksum, Set<String> filtered, File sample) {
        this.url = url;
        this.isDevMode = isDevMode;
        this.isDebugMode = isDebugMode;
        this.shouldIgnoreChecksum = shouldIgnoreChecksum;
        this.filtered = filtered;
        this.sample = sample;
    }

    public void enhance(File sourceJar, File targetJar, File... additionalDependencies) throws IOException {
        List<ClassFileLocator> locators = new ArrayList<ClassFileLocator>();
        locators.add(new ClassFileLocator.ForJarFile(new JarFile(sourceJar)));
        for (File additionalDependency : additionalDependencies) {
            locators.add(new ClassFileLocator.ForJarFile(new JarFile(additionalDependency)));
        }
        locators.add(ClassFileLocator.ForClassLoader.of(JamEnhancer.class.getClassLoader()));
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(locators);

        final ByteBuddy byteBuddy = new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);

        Map<String, byte[]> injections = new HashMap<String, byte[]>();
        Map<String, byte[]> binaries = new HashMap<String, byte[]>();
        Map<String, List<String>> captures = new HashMap<String, List<String>>();

        for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
            if (filtered.contains(accelleration.target()) || !accelleration.isActive(isDevMode)) {
                if (isDebugMode) {
                    System.out.println(accelleration + " is filtered or not active in current mode");
                }
                continue;
            }

            if (isDebugMode) {
                System.out.println("Applying " + accelleration.target() + " onto " + accelleration.type());
            }
            if (!accelleration.checksum(classFileLocator, isDebugMode) && !isDevMode && !shouldIgnoreChecksum) {
                throw new IllegalStateException("Could not apply " + accelleration + " due to check sum mismatch");
            }

            TypeDescription typeDescription = typePool.describe(accelleration.type()).resolve();
            TypeDescription adviceType = typePool.describe(accelleration.target()).resolve();
            Advice.WithCustomMapping advice = Advice.withCustomMapping().bind(DevMode.class, isDevMode);
            DynamicType result = byteBuddy.redefine(typeDescription, classFileLocator).visit((accelleration.isTrivialEnter()
                    ? advice.to(TypeDescription.ForLoadedType.of(TrivialEnterAdvice.class), adviceType, classFileLocator)
                    : advice.to(adviceType, classFileLocator)).on(accelleration.method())).make();

            injections.put(result.getTypeDescription().getInternalName() + ".class", result.getBytes());
            for (Map.Entry<TypeDescription, byte[]> entry : accelleration.inlined().entrySet()) {
                injections.put(entry.getKey().getInternalName() + ".class", entry.getValue());
            }

            MethodAccelleration.StaleBinaries staleBinaries = accelleration.staleBinaries(byteBuddy,
                    Platform.NATIVE_SHARED_OBJ_FOLDER,
                    Platform.NATIVE_SHARED_OBJ_PREFIX,
                    Platform.NATIVE_SHARED_OBJ_EXT,
                    classFileLocator);
            for (DynamicType dynamicType : staleBinaries.types) {
                injections.put(dynamicType.getTypeDescription().getInternalName() + ".class", dynamicType.getBytes());
            }
            binaries.putAll(staleBinaries.binaries);

            for (MethodAccelleration.Capture capture : accelleration.captures()) {
                List<String> fields = captures.get(capture.getName());
                if (fields == null) {
                    fields = new ArrayList<String>();
                }
                fields.addAll(Arrays.asList(capture.getField()));
                captures.put(capture.getName(), fields);
            }

            if (isDebugMode) {
                System.out.println("Registered accelleration: " + accelleration);
            }
        }

        for (Map.Entry<String, List<String>> capture : captures.entrySet()) {
            for (final String field : capture.getValue()) {
                if (isDebugMode) {
                    System.out.println("Applying capture for " + field + " of " + capture.getKey());
                }
                String internalName = capture.getKey().replace('.', '/') + ".class";
                ClassFileLocator locator;
                if (injections.containsKey(internalName)) {
                    locator = new ClassFileLocator.Compound(ClassFileLocator.Simple.of(capture.getKey(), injections.get(internalName)), classFileLocator);
                } else {
                    locator = classFileLocator;
                }

                DynamicType dynamicType = byteBuddy.redefine(typePool.describe(capture.getKey()).resolve(), locator).visit(Advice.withCustomMapping()
                        .bind(CaptureAdvice.CapturedField.class, field)
                        .bind(CaptureAdvice.CapturedValue.class, new Advice.OffsetMapping() {
                            @Override
                            public Target resolve(TypeDescription instrumentedType,
                                                  MethodDescription instrumentedMethod,
                                                  Assigner assigner,
                                                  Advice.ArgumentHandler argumentHandler,
                                                  Sort sort) {
                                return new Target.ForField.ReadOnly(instrumentedType.getDeclaredFields().filter(named(field)).getOnly());
                            }
                        }).to(CaptureAdvice.class).on(isConstructor())).make();
                injections.put(dynamicType.getTypeDescription().getInternalName() + "class", dynamicType.getBytes());
            }
        }

        Set<String> remainingCaptures = new HashSet<String>();
        for (String capture : captures.keySet()) {
            remainingCaptures.add(capture.replace('.', '/') + ".class");
        }

        JarInputStream jarInputStream = new JarInputStream(new BufferedInputStream(new FileInputStream(sourceJar)));
        try {
            if (!targetJar.isFile() && !targetJar.createNewFile()) {
                throw new IllegalArgumentException("Could not create file: " + targetJar);
            }
            Manifest manifest = jarInputStream.getManifest();
            JarOutputStream jarOutputStream = manifest == null
                    ? new JarOutputStream(new FileOutputStream(targetJar))
                    : new JarOutputStream(new FileOutputStream(targetJar), manifest);
            try {
                JarEntry jarEntry;
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    remainingCaptures.remove(jarEntry.getName());
                    byte[] replacement = injections.remove(jarEntry.getName());
                    if (replacement == null) {
                        jarOutputStream.putNextEntry(jarEntry);
                        byte[] buffer = new byte[1024];
                        int index;
                        while ((index = jarInputStream.read(buffer)) != -1) {
                            jarOutputStream.write(buffer, 0, index);
                        }
                    } else {
                        jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                        jarOutputStream.write(replacement);
                    }
                    jarInputStream.closeEntry();
                    jarOutputStream.closeEntry();
                }
                for (Map.Entry<String, byte[]> entry : injections.entrySet()) {
                    jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
                    jarOutputStream.write(entry.getValue());
                    jarOutputStream.closeEntry();
                }
                for (Map.Entry<String, byte[]> binary : binaries.entrySet()) {
                    jarOutputStream.putNextEntry(new JarEntry(binary.getKey()));
                    jarOutputStream.write(binary.getValue());
                    jarOutputStream.closeEntry();
                }
                if (isDevMode) {
                    jarOutputStream.putNextEntry(new JarEntry("META-INF/services/" + JamAgentDispatcher.class.getName()));
                    if (sample == null) {
                        jarOutputStream.write(DispatcherToConsole.class.getName().getBytes("utf-8"));
                        jarOutputStream.closeEntry();
                    } else {
                        jarOutputStream.write(DispatcherToFile.class.getName().getBytes("utf-8"));
                        jarOutputStream.closeEntry();
                        jarOutputStream.putNextEntry(new JarEntry("META-INF/jam/sample.location"));
                        jarOutputStream.write((sample.getAbsolutePath()
                                + "\n" + MAX_OBSERVATION_COUNT_FOR_FILES
                                + "\n" + MAX_OBSERVATION_BYTES_FOR_FILES).getBytes("utf-8"));
                        jarOutputStream.closeEntry();
                    }
                }
                jarOutputStream.putNextEntry(new JarEntry("META-INF/services/" + JamObjectCapture.class.getName()));
                jarOutputStream.write(WeakHashMapCapture.class.getName().getBytes("utf-8"));
                jarOutputStream.closeEntry();
            } finally {
                jarOutputStream.close();
            }
        } finally {
            jarInputStream.close();
        }

        if (!remainingCaptures.isEmpty()) {
            throw new IllegalStateException("Could not find all capture classes in this jar file: " + remainingCaptures);
        }
    }

    public static void main(String[] args) {
        // TODO: add command line interface to trigger enhancement.
    }
}
