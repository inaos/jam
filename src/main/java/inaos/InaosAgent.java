package inaos;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

public class InaosAgent {

    public static void premain(String argument, Instrumentation instrumentation) {
        install(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        install(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }

    private static void install(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
        new AgentBuilder.Default()
                .with(redefinitionStrategy)
                .disableClassFormatChanges()
                .type(declaresMethod(isAnnotatedWith(Accellerate.class)))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module) {
                        return builder
                                .method(isAnnotatedWith(Accellerate.class))
                                .intercept(Advice.to(ExampleAdvice.class)
                                        .wrap(StubMethod.INSTANCE));
                    }
                }).installOn(instrumentation);
    }
}
