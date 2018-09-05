package com.inaos.jam.agent;

import com.inaos.jam.api.DevMode;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;

public class JamEnhancer {

    private final URL url;

    private final boolean isDevMode, isDebugMode;

    private final Set<String> filtered;

    public JamEnhancer(URL url, boolean isDevMode, boolean isDebugMode, Set<String> filtered) {
        this.url = url;
        this.isDevMode = isDevMode;
        this.isDebugMode = isDebugMode;
        this.filtered = filtered;
    }

    public void enhance(File sourceJar, File targetJar, File... additionalDependencies) throws IOException {
        List<ClassFileLocator> locators = new ArrayList<ClassFileLocator>();
        locators.add(new ClassFileLocator.ForJarFile(new JarFile(sourceJar)));
        for (File additionalDependency : additionalDependencies) {
            locators.add(new ClassFileLocator.ForJarFile(new JarFile(additionalDependency)));
        }
        locators.add(ClassFileLocator.ForClassLoader.of(JamEnhancer.class.getClassLoader()));
        ClassFileLocator locator = new ClassFileLocator.Compound(locators);

        // TODO: BINARIES
        // TODO: CAPTURES
        // TODO: BOOT INIT
        // TODO: CHECKSUM

        TypePool typePool = TypePool.Default.WithLazyResolution.of(locator);

        Map<String, byte[]> injections = new HashMap<String, byte[]>();
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
            DynamicType result = new ByteBuddy().redefine(type, locator).visit((accelleration.isTrivialEnter()
                    ? advice.to(TypeDescription.ForLoadedType.of(TrivialEnterAdvice.class), adviceType, locator)
                    : advice.to(adviceType, locator)).on(accelleration.method())).make();

            injections.put(result.getTypeDescription().getInternalName() + ".class", result.getBytes());
            for (Map.Entry<TypeDescription, byte[]> entry : accelleration.inlined().entrySet()) {
                injections.put(entry.getKey().getInternalName() + ".class", entry.getValue());
            }
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
            } finally {
                jarOutputStream.close();
            }
        } finally {
            jarInputStream.close();
        }
    }

}
