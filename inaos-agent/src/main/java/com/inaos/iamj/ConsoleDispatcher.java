package com.inaos.iamj;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.Serializable;

public class ConsoleDispatcher extends InaosAgentDispatcher {

    @Override
    protected void accept(String name, Serializable returned, Serializable[] args) {
        System.out.println(new Observation(name, returned, args));
    }
}
