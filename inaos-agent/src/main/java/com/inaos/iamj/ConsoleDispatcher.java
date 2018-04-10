package com.inaos.iamj;

import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

import java.io.Serializable;

class ConsoleDispatcher extends InaosAgentDispatcher {

    @Override
    protected void accept(String name, Serializable[] args) {
        System.out.println(new Observation(name, args));
    }
}
