package com.inaos.iamj.agent;

import com.esotericsoftware.kryo.Kryo;
import com.inaos.iamj.boot.InaosAgentDispatcher;
import com.inaos.iamj.observation.Observation;

abstract class DispatcherBase extends InaosAgentDispatcher {

    protected final Kryo kryo = new Kryo();

    protected boolean suppressSample(String name) {
        return false;
    }

    @Override
    protected Object doObserve(String name) {
        if (suppressSample(name)) {
            return null;
        }
        return new ObservationBuilder(name);
    }

    @Override
    protected void doAttach(Object observation, String name, Class<?> type, Object argument) {
        if (!(observation instanceof ObservationBuilder)) {
            throw new IllegalArgumentException("Unexpected observation type: " + observation);
        }
        ObservationBuilder builder = (ObservationBuilder) observation;
        builder.serialize(kryo, name, type, argument);
    }

    @Override
    protected void doAttach(Object observation, String name, Class<?>[] types, Object[] arguments) {
        if (!(observation instanceof ObservationBuilder)) {
            throw new IllegalArgumentException("Unexpected observation type: " + observation);
        }
        ObservationBuilder builder = (ObservationBuilder) observation;
        builder.serialize(kryo, name, types, arguments);
    }

    @Override
    protected void doCommit(Object observation) {
        if (!(observation instanceof ObservationBuilder)) {
            throw new IllegalArgumentException("Unexpected observation type: " + observation);
        }
        doCommit(((ObservationBuilder) observation).toObservation());

    }

    protected abstract void doCommit(Observation observation);
}
