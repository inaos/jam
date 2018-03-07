package inaos;

import net.bytebuddy.asm.Advice;

public class ExampleExitAdvice {

    @Advice.OnMethodExit
    static void exit(@Advice.Enter boolean devMode,
                     @Advice.Origin String method,
                     @Advice.Argument(0) int n,
                     @Advice.FieldValue("uniform") UniformRandomNumberGenerator uniform,
                     @Advice.Return(readOnly = false) double[] returned) {
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            r[i] = uniform.random();
        }
        if (devMode) {
            InaosAgentHelper.serialize(method, double[].class, r, returned);
        } else {
            throw new UnsupportedOperationException();
//            NativeDispatcher.exec(r);
//            returned = r;
        }
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Enter boolean devMode,
                     @Advice.Origin String method,
                     @Advice.Argument(0) int n,
                     @Advice.Return(readOnly = false) double[] returned) {
        if (devMode) {
            InaosAgentHelper.serialize(method, int.class, n, returned);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
