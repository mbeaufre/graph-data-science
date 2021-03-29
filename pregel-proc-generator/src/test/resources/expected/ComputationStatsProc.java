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
package org.neo4j.graphalgo.beta.pregel.cc;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.neo4j.graphalgo.AlgoBaseProc;
import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.BaseProc;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.beta.pregel.Pregel;
import org.neo4j.graphalgo.beta.pregel.PregelConfig;
import org.neo4j.graphalgo.beta.pregel.PregelResult;
import org.neo4j.graphalgo.beta.pregel.PregelStatsProc;
import org.neo4j.graphalgo.beta.pregel.PregelStatsResult;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimation;
import org.neo4j.graphalgo.core.utils.progress.ProgressEventTracker;
import org.neo4j.graphalgo.result.AbstractResultBuilder;
import org.neo4j.graphalgo.results.MemoryEstimateResult;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

@Generated("org.neo4j.graphalgo.beta.pregel.PregelProcessor")
public final class ComputationStatsProc extends PregelStatsProc<ComputationAlgorithm, PregelConfig> {
    @Procedure(
            name = "gds.pregel.test.stats",
            mode = Mode.READ
    )
    @Description("Test computation description")
    public Stream<PregelStatsResult> stats(@Name("graphName") Object graphNameOrConfig,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {
        return stats(compute(graphNameOrConfig, configuration));
    }

    @Procedure(
            name = "gds.pregel.test.stats.estimate",
            mode = Mode.READ
    )
    @Description(BaseProc.ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> statsEstimate(@Name("graphName") Object graphNameOrConfig,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {
        return computeEstimate(graphNameOrConfig, configuration);
    }

    @Override
    protected AbstractResultBuilder<PregelStatsResult> resultBuilder(
            AlgoBaseProc.ComputationResult<ComputationAlgorithm, PregelResult, PregelConfig> computeResult) {
        var ranIterations = computeResult.result().ranIterations();
        var didConverge = computeResult.result().didConverge();
        return new PregelStatsResult.Builder().withRanIterations(ranIterations).didConverge(didConverge);
    }

    @Override
    protected PregelConfig newConfig(String username, Optional<String> graphName,
            Optional<GraphCreateConfig> maybeImplicitCreate, CypherMapWrapper config) {
        return PregelConfig.of(username, graphName, maybeImplicitCreate, config);
    }

    @Override
    protected AlgorithmFactory<ComputationAlgorithm, PregelConfig> algorithmFactory() {
        return new AlgorithmFactory<ComputationAlgorithm, PregelConfig>() {
            @Override
            public ComputationAlgorithm build(Graph graph, PregelConfig configuration,
                    AllocationTracker tracker, Log log, ProgressEventTracker eventTracker) {
                return new ComputationAlgorithm(graph, configuration, tracker, log);
            }

            @Override
            public MemoryEstimation memoryEstimation(PregelConfig configuration) {
                var computation = new Computation();
                return Pregel.memoryEstimation(computation.schema(configuration), computation.reducer().isPresent(), configuration.isAsynchronous());
            }
        };
    }
}
