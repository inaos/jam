package com.inaos.iamj;

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

    public static void premain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    public static ResettableClassFileTransformer install(String argument, final Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        try {
            Boolean devMode = null;
            URL url = null;

            for (String config : argument.split(",")) {
                String[] pair = config.split("=");
                if (pair.length != 2) {
                    throw new IllegalArgumentException();
                } else if (pair[0].equals("devMode")) {
                    devMode = Boolean.parseBoolean(pair[1]);
                } else if (pair[0].equals("library")) {
                    url = new URL(pair[1]);
                } else {
                    throw new IllegalArgumentException("Unknown configuratin: " + pair[0]);
                }
            }

            if (url == null) {
                throw new IllegalArgumentException("Agent library is not set");
            }

            final boolean isDevMode = devMode == null ? false : devMode;

//        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile()); // TODO: Boot file injection

            AgentBuilder agentBuilder = new AgentBuilder.Default(BYTE_BUDDY)
//                    .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                    .with(redefinitionStrategy)
                    .disableClassFormatChanges();

            final String extension = File.pathSeparator.equals("\\") ? "dll" : "so";

            for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
                agentBuilder = agentBuilder.type(accelleration.type()).transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        for (DynamicType.Unloaded<?> type : accelleration.binaries(BYTE_BUDDY, extension)) {
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
}
