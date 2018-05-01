package com.inaos.iamj.agent;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

public class ConsoleDispatcher extends InaosAgentDispatcher {

    @Override
    protected void accept(String name, Object returned, Object[] args) {
        System.out.println(new Observation(name, returned, args));
    }
}
