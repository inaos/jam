package com.inaos.jam.agent;

import com.inaos.jam.api.DevMode;
import com.inaos.jam.boot.JamAgentDispatcher;
import com.inaos.jam.boot.JamObjectCapture;
import com.inaos.jam.tool.Platform;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.pool.TypePool;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;

public class JamEnhancer {

    private static final long MAX_OBSERVATION_COUNT_FOR_FILES = 10000;

    private static final long MAX_OBSERVATION_BYTES_FOR_FILES = 1024 * 1024; // 1 MB

    private final URL url;

    private final boolean isDevMode, isDebugMode;

    private final Set<String> filtered;

    private final File sample;

    public JamEnhancer(URL url, boolean isDevMode, boolean isDebugMode, Set<String> filtered, File sample) {
        this.url = url;
        this.isDevMode = isDevMode;
        this.isDebugMode = isDebugMode;
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

        // TODO: BINARIES - SHUTDOWN HOOK
        // TODO: CAPTURES
        // TODO: CHECKSUM

        final ByteBuddy byteBuddy = new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);

        Map<String, byte[]> injections = new HashMap<String, byte[]>();
        Map<String, byte[]> binaries = new HashMap<String, byte[]>();
        for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
            if (filtered.contains(accelleration.target()) || !accelleration.isActive(isDevMode)) {
                if (isDebugMode) {
                    System.out.println(accelleration + " is filtered or not active in current mode");
                }
                continue;
            }

            TypeDescription type = typePool.describe(accelleration.type()).resolve();
            TypeDescription adviceType = typePool.describe(accelleration.target()).resolve();
            Advice.WithCustomMapping advice = Advice.withCustomMapping().bind(DevMode.class, isDevMode);
            DynamicType result = byteBuddy.redefine(type, classFileLocator).visit((accelleration.isTrivialEnter()
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
    }

    public static void main(String[] args) {
        // TODO: add command line interface to trigger enhancement.
    }
}
