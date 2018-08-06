package com.inaos.jam.benchmark;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.inaos.jam.observation.Observation;
import com.inaos.jam.observation.SerializedValue;
import com.inaos.jam.observation.kryo.KryoSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.openjdk.jmh.annotations.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@State(Scope.Benchmark)
public class JamObservations {

    private final List<Map<String, Object[]>> elements = new ArrayList<Map<String, Object[]>>();

    @Param(value = {})
    private String source, name;

    @Setup(Level.Trial)
    public void setup() {
        try {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            KryoSerializer kryoSerializer = new KryoSerializer(kryo);
            Input in = new Input(new FileInputStream(source));

            try {
                while (!in.eof()) {
                    Observation observation = kryo.readObject(in, Observation.class);
                    if (observation.getName().equals(name)) {
                        Map<String, Object[]> observations = new HashMap<String, Object[]>();
                        for (Map.Entry<String, SerializedValue> entry : observation.getValues().entrySet()) {
                            observations.put(entry.getKey(), kryoSerializer.resolveArguments(entry.getValue()));
                        }
                        elements.add(Collections.unmodifiableMap(observations));
                    }
                }
            } finally {
                in.close();
            }
            if (elements.isEmpty()) {
                throw new IllegalStateException("No benchmarks found in " + source + " for " + name);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public List<Map<String, Object[]>> getElements() {
        return Collections.unmodifiableList(elements);
    }
}
