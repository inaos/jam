package com.inaos.iamj.utility;

import com.inaos.iamj.observation.Observation;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

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

        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(observation.getMethodName())
                .addModifiers(Modifier.STATIC, Modifier.NATIVE)
                .returns(observation.getReturnType());

        int index = 0;
        for (Class<?> argumentType : observation.getArgumentTypes()) {
            methodBuilder = methodBuilder.addParameter(argumentType, "arg" + index++);
        }

        int packageIndex = observation.getDispatcherName().lastIndexOf('.');
        if (packageIndex == -1) {
            throw new IllegalArgumentException("No package for: " + observation.getDispatcherName());
        }

        TypeSpec dispatcher = TypeSpec.classBuilder(observation.getDispatcherName().substring(packageIndex + 1)).addMethod(methodBuilder.build()).build();
        JavaFile javaFile = JavaFile.builder(observation.getDispatcherName().substring(0, packageIndex), dispatcher).build();

        javaFile.writeTo(folder);
    }
}
