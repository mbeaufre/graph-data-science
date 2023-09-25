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
package org.neo4j.gds.applications.graphstorecatalog;

import java.util.List;

public class GraphStreamNodePropertyResult {
    public final long nodeId;
    public final Object propertyValue;
    public final List<String> nodeLabels;

    public GraphStreamNodePropertyResult(long nodeId, Object propertyValue, List<String> nodeLabels) {
        this.nodeId = nodeId;
        this.propertyValue = propertyValue;
        this.nodeLabels = nodeLabels;
    }
}
