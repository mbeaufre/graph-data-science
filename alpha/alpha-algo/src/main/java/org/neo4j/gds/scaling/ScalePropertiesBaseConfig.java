/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.scaling;

import org.neo4j.graphalgo.PropertyMapping;
import org.neo4j.graphalgo.annotation.Configuration;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.config.AlgoBaseConfig;

import java.util.List;
import java.util.stream.Collectors;

import static org.neo4j.graphalgo.AbstractPropertyMappings.fromObject;

@ValueClass
public interface ScalePropertiesBaseConfig extends AlgoBaseConfig {

    @Configuration.ConvertWith("parsePropertyNames")
    List<String> nodeProperties();

    @Configuration.ConvertWith("org.neo4j.gds.scaling.Scaler.Variant#lookup")
    @Configuration.ToMapValue("org.neo4j.gds.scaling.Scaler.Variant#toString")
    Scaler.Variant scaler();

    @SuppressWarnings("unused")
    static List<String> parsePropertyNames(Object nodePropertiesOrMappings) {
        return fromObject(nodePropertiesOrMappings)
            .mappings()
            .stream()
            .map(PropertyMapping::propertyKey)
            .collect(Collectors.toList());
    }
}
