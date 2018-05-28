/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import java.util.Arrays;
import java.util.Map;

import com.inaos.jam.observation.Observation;
import com.inaos.jam.observation.SerializedValue;

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
