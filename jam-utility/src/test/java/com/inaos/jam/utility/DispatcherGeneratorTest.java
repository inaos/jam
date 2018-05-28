/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.utility;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;

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