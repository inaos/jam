package com.inaos.iamj;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.utility.JavaModule;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class InaosAgent {

    private static final Method LOAD;

    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

    static {
        try {
            LOAD = System.class.getMethod("load", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void premain(String argument, Instrumentation instrumentation) throws Exception {
        install(instrumentation, new URL(argument), AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) throws Exception {
        install(instrumentation, new URL(argument), AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    private static void install(final Instrumentation instrumentation, URL url, AgentBuilder.RedefinitionStrategy redefinitionStrategy) throws Exception {
//        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile()); // TODO: Boot file injection

        AgentBuilder agentBuilder = new AgentBuilder.Default(BYTE_BUDDY)
//                .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .with(redefinitionStrategy)
                .disableClassFormatChanges();

        final String executable = File.pathSeparator.equals("\\") ? ".dll" : ".so";
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        } else if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        for (final MethodAccelleration accelleration : MethodAccelleration.findAll(url)) {
            agentBuilder = agentBuilder.type(accelleration.type()).transform(new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                        TypeDescription typeDescription,
                                                        ClassLoader classLoader,
                                                        JavaModule module) {
                    try {
                        ClassLoadingStrategy<ClassLoader> strategy = ClassLoadingStrategy.Default.INJECTION;
                        strategy.load(classLoader, accelleration.injection());
                        Implementation implementation = StubMethod.INSTANCE;
                        for (String library : accelleration.libraries()) {
                            File file = new File(temp, library + executable);
                            InputStream in = accelleration.resourceAsStream(library + executable);
                            try {
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
                            } finally {
                                in.close();
                            }
                            implementation = MethodCall.invoke(LOAD).with(file.getAbsolutePath()).andThen(implementation);
                        }
                        BYTE_BUDDY.subclass(Runnable.class)
                                .method(named("run"))
                                .intercept(implementation)
                                .make()
                                .load(classLoader, strategy)
                                .getLoaded()
                                .getConstructor()
                                .newInstance()
                                .run();
                        return builder.visit(accelleration.getAdvice().on(accelleration.method()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        agentBuilder.installOn(instrumentation);
    }
}
