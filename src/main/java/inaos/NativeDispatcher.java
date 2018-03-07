package inaos;

public class NativeDispatcher {

    static {
        System.load("/home/rafael/workspace/inaos/iamj/build/libsample.so");
    }

    static native void exec(double[] array);
}
