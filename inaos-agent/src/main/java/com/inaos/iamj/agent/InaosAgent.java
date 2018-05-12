package com.inaos.iamj.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class InaosAgent {

    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

    private static final String OS_NAME = getSystemProperty("os.name");

    private static final String OS_ARCH = getSystemProperty("os.arch");

    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    private static final boolean IS_OS_LINUX = isOsMatchesName("Linux") || isOsMatchesName("LINUX");

    private static final boolean IS_OS_WINDOWS = isOsMatchesName(OS_NAME_WINDOWS_PREFIX);

    private static final boolean IS_OS_ARCH_64 = OS_ARCH.equals("amd64");

    private static final boolean IS_OS_ARCH_32 = OS_ARCH.equals("x86");

    private static final String NATIVE_SHARED_OBJ_EXT;

    private static final String NATIVE_SHARED_OBJ_PREFIX;

    private static final String NATIVE_SHARED_OBJ_FOLDER;

    private static final long MAX_OBSERVATION_COUNT_FOR_FILES = 10000;

    private static final long MAX_OBSERVATION_BYTES_FOR_FILES = 1024 * 1024; // 1 MB

    static {
        if (IS_OS_LINUX) {
            NATIVE_SHARED_OBJ_EXT = "so";
            NATIVE_SHARED_OBJ_PREFIX = "lib";
            if (IS_OS_ARCH_64) {
                NATIVE_SHARED_OBJ_FOLDER = "linux-amd64";
            } else if (IS_OS_ARCH_32) {
                NATIVE_SHARED_OBJ_FOLDER = "linux-i368";
            } else {
                throw new InaosAgentException("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else if (IS_OS_WINDOWS) {
            NATIVE_SHARED_OBJ_EXT = "dll";
            NATIVE_SHARED_OBJ_PREFIX = "";
            if (IS_OS_ARCH_64) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-amd64";
            } else if (IS_OS_ARCH_32) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-x86";
            } else {
                throw new InaosAgentException("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else {
            throw new InaosAgentException("Operating System not supported: " + OS_NAME);
        }
    }

    public static void premain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    public static ResettableClassFileTransformer install(String argument, Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        try {
            Boolean devMode = null;
            URL url = null;
            File sample = null;

            InputStream bootJar = InaosAgent.class.getResourceAsStream("/inaos-boot.jar");
            if (bootJar == null) {
                throw new IllegalStateException("Boot jar not found");
            }
            File materializedBootJar;
            try {
                materializedBootJar = File.createTempFile("inaos-boot", ".jar");
                OutputStream out = new FileOutputStream(materializedBootJar);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = bootJar.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    out.close();
                }
            } finally {
                bootJar.close();
            }

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(materializedBootJar));

            for (String config : argument.split(",")) {
                String[] pair = config.split("=");
                if (pair.length != 2) {
                    throw new IllegalArgumentException();
                } else if (pair[0].equals("devMode")) {
                    devMode = Boolean.parseBoolean(pair[1]);
                } else if (pair[0].equals("library")) {
                    url = new URL(pair[1]);
                } else if (pair[0].equals("sample")) {
                    sample = new File(pair[1]);
                } else {
                    throw new IllegalArgumentException("Unknown configuration: " + pair[0]);
                }
            }

            if (url == null) {
                throw new IllegalArgumentException("Agent library is not set");
            }

            final boolean isDevMode = devMode == null ? false : devMode;
            if (isDevMode) {
                registerDispatcher(sample);
            }

            AgentBuilder agentBuilder = new AgentBuilder.Default(BYTE_BUDDY)
                    //.with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                    .with(redefinitionStrategy)
                    .disableClassFormatChanges();

            final Collection<Runnable> destructions = Collections.newSetFromMap(new ConcurrentHashMap<Runnable, Boolean>());
            if (!isDevMode) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        for (Runnable destruction : destructions) {
                            destruction.run();
                        }
                    }
                });
            }

            for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
                agentBuilder = agentBuilder.type(accelleration.type()).transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        MethodAccelleration.Binaries binaries = accelleration.binaries(BYTE_BUDDY, NATIVE_SHARED_OBJ_FOLDER, NATIVE_SHARED_OBJ_PREFIX, NATIVE_SHARED_OBJ_EXT);
                        for (DynamicType.Unloaded<?> type : binaries.types) {
                            type.load(classLoader, ClassLoadingStrategy.Default.INJECTION.allowExistingTypes());
                        }
                        if (!isDevMode) {
                            destructions.addAll(binaries.destructions);
                        }
                        return builder.visit(accelleration.advice(isDevMode).on(accelleration.method()));
                    }
                }).asDecorator();
            }

            return agentBuilder.installOn(instrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Use reflection for delayed class resolution after appending to boot loader.
    private static void registerDispatcher(File sample) throws Exception {
        Object which;
        if (sample == null) {
            which = Class.forName("com.inaos.iamj.agent.DispatcherToConsole")
                    .getConstructor()
                    .newInstance();
        } else {
            which = Class.forName("com.inaos.iamj.agent.DispatcherToFile")
                    .getConstructor(File.class, long.class, long.class)
                    .newInstance(sample, MAX_OBSERVATION_COUNT_FOR_FILES, MAX_OBSERVATION_BYTES_FOR_FILES);
        }
        Class<?> dispatcher = Class.forName("com.inaos.iamj.boot.InaosAgentDispatcher");
        Field instance = dispatcher.getField("dispatcher");
        instance.set(null, which);
    }

    private static boolean isOsMatchesName(String osNamePrefix) {
        return isOSNameMatch(OS_NAME, osNamePrefix);
    }

    private static boolean isOSNameMatch(String osName, String osNamePrefix) {
        if (osName == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix);
    }

    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            System.err.println("Caught a SecurityException reading the system property '" + property + "'; the SystemUtils property value will default to null.");
            return null;
        }
    }
}
