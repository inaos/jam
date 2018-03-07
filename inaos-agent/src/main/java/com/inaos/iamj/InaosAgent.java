package com.inaos.iamj;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

public class InaosAgent {

    public static void premain(String argument, Instrumentation instrumentation) {
        install(instrumentation, "dev".equals(argument), AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(instrumentation, "dev".equals(argument), AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    private static void install(Instrumentation instrumentation,
                                final boolean devMode,
                                AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        new AgentBuilder.Default()
                .with(redefinitionStrategy)
                .disableClassFormatChanges()
//                .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .type(declaresMethod(isAnnotatedWith(Accellerate.class)))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        return builder.visit(Advice
                                .withCustomMapping()
                                .bind(DevMode.class, devMode)
                                .to(EnterAdvice.class, ExampleExitAdvice.class)
                                .on(isAnnotatedWith(Accellerate.class)));
                    }
                }).installOn(instrumentation);
    }
}
