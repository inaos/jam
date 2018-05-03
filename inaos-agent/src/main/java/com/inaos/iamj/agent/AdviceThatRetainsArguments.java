package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

class AdviceThatRetainsArguments {

    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    static Object enter(@DevMode boolean devMode, @Advice.Origin Method method, @Advice.AllArguments Object[] arguments) {
        if (devMode) {
            return InaosAgentDispatcher.serialize(method.getParameterTypes(), arguments);
        } else {
            return null;
        }
    }
}
