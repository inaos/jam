package com.inaos.iamj.utility;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.inaos.iamj.observation.Observation;
import com.inaos.iamj.observation.SerializedValue;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DispatcherGeneratorTest {

    @Test
    public void testDispatcherGeneration() throws Exception {
        Kryo kryo = new Kryo();

        // TODO: Is this dispatcher generation really meaningful?

//        Observation observation = new Observation("foo", "qux.Baz", "bar",
//                SerializedValue.make(kryo, new Class<?>[]{String.class, long.class}, new Object[]{"abc", 123L}),
//                null,
//                SerializedValue.make(kryo, int.class, 42));
//
//        File test = File.createTempFile("iamj", ".tmp");
//        File folder = new File(test.getParentFile(), "test_target_" + new Random().nextInt());
//        if (!folder.mkdir()) {
//            throw new AssertionError();
//        }
//
//        Output out = new Output(new FileOutputStream(test));
//        try {
//            kryo.writeClassAndObject(out, observation);
//            kryo.writeClassAndObject(out, observation);
//        } finally {
//            out.close();
//        }
//
//        Main.main("--dispatcher", "--source", test.getAbsolutePath(), "--target", folder.getAbsolutePath());
//
//        File expectedClass = new File(folder, "qux/Baz.java");
//        assertThat(expectedClass.isFile(), is(true));
//
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        assertThat(compiler.run(null, null, null, expectedClass.getAbsolutePath()), is(0));
    }
}