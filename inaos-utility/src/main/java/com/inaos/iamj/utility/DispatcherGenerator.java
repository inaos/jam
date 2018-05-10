package com.inaos.iamj.utility;

import com.inaos.iamj.observation.Observation;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class DispatcherGenerator {

    private final Set<String> duplicates = new HashSet<String>();

    void generateDispatcher(Observation observation, File folder) throws IOException {
        if (!duplicates.add(observation.getName())) {
            return;
        }

//        MethodSpec.Builder methodBuilder = MethodSpec
//                .methodBuilder(observation.getMethodName())
//                .addModifiers(Modifier.STATIC, Modifier.NATIVE)
//                .returns(observation.getSerializedReturn() == null ? TypeName.VOID : of(observation.getSerializedReturn().getTypes()[0]));
//
//        int index = 0;
//        for (String argumentType : observation.getSerializedArguments().getTypes()) {
//            methodBuilder = methodBuilder.addParameter(of(argumentType), "arg" + index++);
//        }
//
//        ClassName className = ClassName.bestGuess(observation.getDispatcherName());
//        TypeSpec dispatcher = TypeSpec.classBuilder(className).addMethod(methodBuilder.build()).build();
//        JavaFile javaFile = JavaFile.builder(className.packageName(), dispatcher).build();
//
//        javaFile.writeTo(folder);
    }

    private static TypeName of(String name) {
        if (name.equals(boolean.class.getName())) return TypeName.BOOLEAN;
        if (name.equals(byte.class.getName())) return TypeName.BYTE;
        if (name.equals(short.class.getName())) return TypeName.SHORT;
        if (name.equals(int.class.getName())) return TypeName.INT;
        if (name.equals(long.class.getName())) return TypeName.LONG;
        if (name.equals(char.class.getName())) return TypeName.CHAR;
        if (name.equals(float.class.getName())) return TypeName.FLOAT;
        if (name.equals(double.class.getName())) return TypeName.DOUBLE;
        return ClassName.bestGuess(name);
    }
}
