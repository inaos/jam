package com.inaos.jam.agent;

import com.inaos.jam.boot.JamObjectCapture;
import net.bytebuddy.asm.Advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class CaptureAdvice {

    @Advice.OnMethodExit
    static void exit(@Advice.This Object self,
                     @CapturedField String field,
                     @CapturedValue Object value) {
        JamObjectCapture.capture(self, field, value);
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CapturedField {

    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CapturedValue {

    }
}
