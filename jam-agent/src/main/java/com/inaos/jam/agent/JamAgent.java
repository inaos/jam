package com.inaos.jam.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
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

import com.inaos.jam.api.DevMode;

public class JamAgent {

    private static final String OS_NAME = getSystemProperty("os.name");

    private static final String OS_ARCH = getSystemProperty("os.arch");

    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    private static final boolean IS_OS_LINUX = isOsMatchesName("Linux") || isOsMatchesName("LINUX");

    private static final boolean IS_OS_WINDOWS = isOsMatchesName(OS_NAME_WINDOWS_PREFIX);

    private static final boolean IS_OS_ARCH_64 = "amd64".equals(OS_ARCH);

    private static final boolean IS_OS_ARCH_32 = "x86".equals(OS_ARCH);

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
                NATIVE_SHARED_OBJ_FOLDER = null;
                System.err.println("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else if (IS_OS_WINDOWS) {
            NATIVE_SHARED_OBJ_EXT = "dll";
            NATIVE_SHARED_OBJ_PREFIX = "";
            if (IS_OS_ARCH_64) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-amd64";
            } else if (IS_OS_ARCH_32) {
                NATIVE_SHARED_OBJ_FOLDER = "win32-x86";
            } else {
                NATIVE_SHARED_OBJ_FOLDER = null;
                System.err.println("Operating System Architecture not supported: " + OS_ARCH);
            }
        } else {
            NATIVE_SHARED_OBJ_EXT = null;
            NATIVE_SHARED_OBJ_PREFIX = null;
            NATIVE_SHARED_OBJ_FOLDER = null;
            System.err.println("Operating System not supported: " + OS_NAME);
        }
    }

    public static void premain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    public static ResettableClassFileTransformer install(String argument, Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        if (NATIVE_SHARED_OBJ_EXT == null || NATIVE_SHARED_OBJ_PREFIX == null || NATIVE_SHARED_OBJ_FOLDER == null) {
            return null;
        }
        try {
            Boolean devMode = null;
            Boolean expectedName = null;
			Boolean debugMode = null;
            URL url = null;
            File sample = null;

            InputStream bootJar = JamAgent.class.getResourceAsStream("/jam-boot.jar");
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
                } else if (pair[0].equals("expectedName")) {
                    expectedName = Boolean.parseBoolean(pair[1]);
                } else if (pair[0].equals("library")) {
                    url = new URL(pair[1]);
                } else if (pair[0].equals("sample")) {
                    sample = new File(pair[1]);
				} else if (pair[0].equals("debugMode")) {
					debugMode = Boolean.parseBoolean(pair[1]);
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
            final boolean isExpectedName = expectedName == null ? true : expectedName;
			final boolean isDebugMode = debugMode == null ? false : debugMode;

			final ByteBuddy byteBuddy = new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

			AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy)
                    .with(redefinitionStrategy)
                    .disableClassFormatChanges();
			if (isDebugMode) {
			    agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly());
			}

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

            final ClassLoadingStrategy<ClassLoader> classLoadingStrategy = ClassLoadingStrategy.Default.INJECTION.allowExistingTypes();
            for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
                AgentBuilder.Transformer.ForAdvice adviceTransformer = new AgentBuilder.Transformer.ForAdvice(Advice.withCustomMapping()
                        .bind(DevMode.class, isDevMode))
                        .include(accelleration.classFileLocator());
                if (accelleration.isTrivialEnter()) {
                    adviceTransformer = adviceTransformer.advice(accelleration.method(), TrivialEnterAdvice.class.getName(), accelleration.target());
                } else {
                    adviceTransformer = adviceTransformer.advice(accelleration.method(), accelleration.target());
                }
                agentBuilder = agentBuilder.type(accelleration.type(!isExpectedName)).transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        MethodAccelleration.Binaries binaries = accelleration.binaries(byteBuddy,
                                NATIVE_SHARED_OBJ_FOLDER,
                                NATIVE_SHARED_OBJ_PREFIX,
                                NATIVE_SHARED_OBJ_EXT,
                                classLoader);
                        for (DynamicType.Unloaded<?> type : binaries.types) {
                            type.load(classLoader, classLoadingStrategy);
                        }
                        if (!isDevMode) {
                            classLoadingStrategy.load(classLoader, accelleration.inlined());
                            destructions.addAll(binaries.destructions);
                        }
                        return builder;
                    }
                }).transform(adviceTransformer).asDecorator();
             if (isDebugMode) {
                 System.out.println("Registered accelleration: " + accelleration);
             }
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
            which = Class.forName("com.inaos.jam.agent.DispatcherToConsole")
                    .getConstructor()
                    .newInstance();
        } else {
            which = Class.forName("com.inaos.jam.agent.DispatcherToFile")
                    .getConstructor(File.class, long.class, long.class)
                    .newInstance(sample, MAX_OBSERVATION_COUNT_FOR_FILES, MAX_OBSERVATION_BYTES_FOR_FILES);
        }
        Class<?> dispatcher = Class.forName("com.inaos.jam.boot.JamAgentDispatcher");
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
