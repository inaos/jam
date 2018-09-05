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

import com.inaos.jam.api.DevMode;
import com.inaos.jam.tool.Platform;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class JamAgent {

    private static final long MAX_OBSERVATION_COUNT_FOR_FILES = 10000;

    private static final long MAX_OBSERVATION_BYTES_FOR_FILES = 1024 * 1024; // 1 MB

    public static void premain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(argument, instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    private static ResettableClassFileTransformer install(String argument, Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        if (Platform.NATIVE_SHARED_OBJ_EXT == null || Platform.NATIVE_SHARED_OBJ_PREFIX == null || Platform.NATIVE_SHARED_OBJ_FOLDER == null) {
            return null;
        }
        try {
            Boolean devMode = null;
            Boolean expectedName = null;
            Boolean debugMode = null;
            Boolean ignoreChecksum = null;
            URL url = null;
            File sample = null;
            Set<String> filtered = new HashSet<String>();

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
                } else if (pair[0].equals("filter")) {
                    filtered.add(pair[1]);
                } else if (pair[0].equals("ignoreChecksum")) {
                    ignoreChecksum = Boolean.parseBoolean(pair[1]);
                } else {
                    throw new IllegalArgumentException("Unknown configuration: " + pair[0]);
                }
            }

            if (url == null) {
                throw new IllegalArgumentException("Agent library is not set");
            }

            final boolean isDevMode = devMode == null ? false : devMode;
            final boolean isExpectedName = expectedName == null ? true : expectedName;
            final boolean isDebugMode = debugMode == null ? false : debugMode;
            final boolean shouldIgnoreChecksum = ignoreChecksum == null ? false : ignoreChecksum;

            if (isDevMode) {
                registerDispatcher(sample);
            }
            registerCapture();

            final ByteBuddy byteBuddy = new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

            AgentBuilder builder = new AgentBuilder.Default(byteBuddy)
                    .with(redefinitionStrategy)
                    .disableClassFormatChanges();
            if (isDebugMode) {
                builder = builder.with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly());
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
                if (filtered.contains(accelleration.target()) || !accelleration.isActive(isDevMode)) {
                    if (isDebugMode) {
                        System.out.println(accelleration + " is filtered or not active in current mode");
                    }
                    continue;
                }
                AgentBuilder.Transformer.ForAdvice adviceTransformer = new AgentBuilder.Transformer.ForAdvice(Advice.withCustomMapping()
                        .bind(DevMode.class, isDevMode))
                        .include(accelleration.classFileLocator());
                if (accelleration.isTrivialEnter()) {
                    adviceTransformer = adviceTransformer.advice(accelleration.method(), TrivialEnterAdvice.class.getName(), accelleration.target());
                } else {
                    adviceTransformer = adviceTransformer.advice(accelleration.method(), accelleration.target());
                }
                builder = builder.type(accelleration.typeMatcher(!isExpectedName)).transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        if (isDebugMode) {
                            System.out.println("Applying " + accelleration.target() + " onto " + typeDescription);
                        }
                        if (!accelleration.checksum(ClassFileLocator.ForClassLoader.of(classLoader), isDebugMode) && !isDevMode && !shouldIgnoreChecksum) {
                            throw new IllegalStateException("Could not apply " + accelleration + " due to check sum mismatch");
                        }
                        MethodAccelleration.LiveBinaries binaries = accelleration.liveBinaries(byteBuddy,
                                Platform.NATIVE_SHARED_OBJ_FOLDER,
                                Platform.NATIVE_SHARED_OBJ_PREFIX,
                                Platform.NATIVE_SHARED_OBJ_EXT,
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
                for (MethodAccelleration.Capture capture : accelleration.captures()) {
                    AgentBuilder.Identified.Extendable identified = builder.type(named(capture.getName())).transform(AgentBuilder.Transformer.NoOp.INSTANCE);
                    for (final String field : capture.getField()) {
                        identified = identified.transform(new AgentBuilder.Transformer() {
                            @Override
                            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                                    TypeDescription typeDescription,
                                                                    ClassLoader classLoader,
                                                                    JavaModule module) {
                                if (isDebugMode) {
                                    System.out.println("Applying capture for " + field + " of " + typeDescription + " for " + accelleration);
                                }
                                return builder.visit(Advice.withCustomMapping()
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
                                        }).to(CaptureAdvice.class).on(isConstructor()));
                            }
                        });
                        builder = identified.asDecorator();
                    }
                }
                if (isDebugMode) {
                    System.out.println("Registered accelleration: " + accelleration);
                }
            }
            return builder.installOn(instrumentation);
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

    // Use reflection for delayed class resolution after appending to boot loader.
    private static void registerCapture() throws Exception {
        Class<?> dispatcher = Class.forName("com.inaos.jam.boot.JamObjectCapture");
        Field instance = dispatcher.getField("dispatcher");
        instance.set(null, Class.forName("com.inaos.jam.agent.WeakHashMapCapture")
                .getConstructor()
                .newInstance());
    }
}
