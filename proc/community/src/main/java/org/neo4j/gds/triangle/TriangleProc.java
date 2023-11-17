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
package org.neo4j.gds.triangle;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;

public class TriangleProc extends BaseProc {

    static final String DESCRIPTION = "Triangles streams the nodeIds of each triangle in the graph.";

    @Procedure(name = "gds.triangles", mode = READ)
    @Description(DESCRIPTION)
    public Stream<TriangleStream.Result> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new TriangleStreamSpecification(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Procedure(name = "gds.alpha.triangles", mode = READ, deprecatedBy = "gds.triangles")
    @Description(DESCRIPTION)
    @Internal
    @Deprecated(forRemoval = true)
    public Stream<TriangleStream.Result> alphaStream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.alpha.triangles");

        executionContext()
            .log()
            .warn(
                "Procedure `gds.alpha.triangles` has been deprecated, please use `gds.triangles`.");

        return stream(graphName, configuration);
    }
}
