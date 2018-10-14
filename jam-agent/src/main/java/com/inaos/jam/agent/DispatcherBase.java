/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import com.esotericsoftware.kryo.Kryo;
import com.inaos.jam.boot.JamAgentDispatcher;
import com.inaos.jam.observation.Observation;

abstract class DispatcherBase extends JamAgentDispatcher {

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
