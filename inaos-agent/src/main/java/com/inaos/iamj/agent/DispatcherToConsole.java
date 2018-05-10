package com.inaos.iamj.agent;

import com.inaos.iamj.observation.Observation;
import com.inaos.iamj.observation.SerializedValue;

import java.util.Arrays;
import java.util.Map;

public class DispatcherToConsole extends DispatcherBase {

    @Override
    protected void doCommit(Observation observation) {
        StringBuilder sb = new StringBuilder().append("Observation for '").append(observation.getName()).append("'");
        for (Map.Entry<String, SerializedValue> entry : observation.getValues().entrySet()) {
            sb.append("\n")
                    .append(" -> Recorded value with key '").append(entry.getKey()).append("'")
                    .append(" and arguments of types ").append(Arrays.toString(entry.getValue().getTypes()));
        }
        System.out.println(sb.toString());
    }
}
