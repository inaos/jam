package com.inaos.iamj;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;

public class InaosAgent {

    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

    private static final String OS_NAME = getSystemProperty("os.name");
    private static final String OS_ARCH = getSystemProperty("os.arch");
    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";
    private static final boolean IS_OS_LINUX = getOsMatchesName("Linux") || getOsMatchesName("LINUX");
    private static final boolean IS_OS_WINDOWS = getOsMatchesName(OS_NAME_WINDOWS_PREFIX);
    private static final boolean IS_OS_ARCH_64 = OS_ARCH.equals("amd64");
    private static final boolean IS_OS_ARCH_32 = OS_ARCH.equals("x86");

    private static final String NATIVE_SHARED_OBJ_EXT;
    private static final String NATIVE_SHARED_OBJ_PREFIX;
    private static final String NATIVE_SHARED_OBJ_FOLDER;

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
                InaosAgentDispatcher.dispatcher = sample == null ? new ConsoleDispatcher() : new FileWritingDispatcher(sample);
            }

//        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile()); // TODO: Boot file injection

            AgentBuilder agentBuilder = new AgentBuilder.Default(BYTE_BUDDY)
//                    .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                    .with(redefinitionStrategy)
                    .disableClassFormatChanges();

            for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
                agentBuilder = agentBuilder.type(accelleration.type()).transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        for (DynamicType.Unloaded<?> type : accelleration.binaries(BYTE_BUDDY, NATIVE_SHARED_OBJ_FOLDER, NATIVE_SHARED_OBJ_PREFIX, NATIVE_SHARED_OBJ_EXT)) {
                            type.load(classLoader, ClassLoadingStrategy.Default.INJECTION);
                        }
                        return builder.visit(accelleration.advice(isDevMode).on(accelleration.method()));
                    }
                });
            }

            return agentBuilder.installOn(instrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean getOsMatchesName(String osNamePrefix) {
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
