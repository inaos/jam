package com.inaos.iamj.agent;

import net.bytebuddy.asm.Advice;

public class EnterAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    static boolean enter(@DevMode boolean devMode) {
        return devMode;
    }
}
