package com.inaos.iamj.agent;

import net.bytebuddy.asm.Advice;

class AdviceThatIsTrivial {

    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    static boolean enter(@DevMode boolean devMode) {
        return devMode;
    }
}
