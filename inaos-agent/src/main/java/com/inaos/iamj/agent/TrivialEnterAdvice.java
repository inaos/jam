package com.inaos.iamj.agent;

import com.inaos.iamj.api.DevMode;
import net.bytebuddy.asm.Advice;

class TrivialEnterAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    static boolean enter(@DevMode boolean devMode) {
        return devMode;
    }
}
