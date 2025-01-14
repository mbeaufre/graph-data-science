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
package org.neo4j.gds.procedures.algorithms.pathfinding;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.pathfinding.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.pathfinding.ResultBuilder;
import org.neo4j.gds.applications.algorithms.pathfinding.SideEffectProcessingCounts;
import org.neo4j.gds.paths.bellmanford.BellmanFordResult;
import org.neo4j.gds.paths.bellmanford.BellmanFordStatsConfig;

import java.util.Optional;
import java.util.stream.Stream;

class BellmanFordResultBuilderForStatsMode implements ResultBuilder<BellmanFordStatsConfig, BellmanFordResult, Stream<BellmanFordStatsResult>> {
    @Override
    public Stream<BellmanFordStatsResult> build(
        Graph graph,
        GraphStore graphStore,
        BellmanFordStatsConfig configuration,
        Optional<BellmanFordResult> result,
        AlgorithmProcessingTimings timings,
        SideEffectProcessingCounts counts
    ) {
        var statsResult = new BellmanFordStatsResult(
            timings.preProcessingMillis,
            timings.computeMillis,
            0,
            configuration.toMap(),
            result.map(BellmanFordResult::containsNegativeCycle).orElse(false)
        );

        return Stream.of(statsResult);
    }
}
