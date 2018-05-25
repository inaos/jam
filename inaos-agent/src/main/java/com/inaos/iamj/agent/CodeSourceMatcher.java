package com.inaos.iamj.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.List;

class CodeSourceMatcher implements AgentBuilder.RawMatcher {

    private final AgentBuilder.RawMatcher previous;

    private final List<String> expected;

    CodeSourceMatcher(AgentBuilder.RawMatcher previous, List<String> expected) {
        this.previous = previous;
        this.expected = expected;
    }

    @Override
    public boolean matches(TypeDescription typeDescription,
                           ClassLoader classLoader,
                           JavaModule module,
                           Class<?> classBeingRedefined,
                           ProtectionDomain protectionDomain) {
        if (!previous.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
            return false;
        }
        URL location = null;
        if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
            location = protectionDomain.getCodeSource().getLocation();
        }
        if (location != null) {
            File file;
            try {
                file = new File(location.toURI());
            } catch (URISyntaxException e) {
                file = new File(location.getPath());
            }
            String name = file.getName();
            for (String candidate : expected) {
                if (name.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
