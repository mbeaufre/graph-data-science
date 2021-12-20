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
package org.neo4j.gds.ml.linkmodels.pipeline.predict;

import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.gds.GraphStoreAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.model.ModelCatalog;
import org.neo4j.gds.ml.linkmodels.LinkPredictionResult;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineCompanion;
import org.neo4j.gds.pipeline.ComputationResultConsumer;
import org.neo4j.gds.pipeline.GdsCallable;
import org.neo4j.gds.pipeline.validation.ValidationConfiguration;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineCompanion.PREDICT_DESCRIPTION;
import static org.neo4j.gds.pipeline.ExecutionMode.STREAM;

@GdsCallable(name = "gds.alpha.ml.pipeline.linkPrediction.predict.stream", description = PREDICT_DESCRIPTION, executionMode = STREAM)
public class LinkPredictionPipelineStreamProc extends AlgoBaseProc<LinkPredictionPredictPipelineExecutor, LinkPredictionResult, LinkPredictionPredictPipelineStreamConfig, LinkPredictionPipelineStreamProc.Result> {

    @Context
    public ModelCatalog modelCatalog;

    @Procedure(name = "gds.alpha.ml.pipeline.linkPrediction.predict.stream", mode = Mode.READ)
    @Description(PREDICT_DESCRIPTION)
    public Stream<Result> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        // TODO: this will go away once node property steps do not rely on this method
        configuration.put("graphName", graphName);
        var result = compute(graphName, configuration);
        return computationResultConsumer().consume(result, executionContext());
    }

    @Override
    public ValidationConfiguration<LinkPredictionPredictPipelineStreamConfig> validationConfig() {
        return LinkPredictionPipelineCompanion.getValidationConfig();
    }

    @Override
    protected LinkPredictionPredictPipelineStreamConfig newConfig(String username, CypherMapWrapper config) {
        return LinkPredictionPredictPipelineStreamConfig.of(username, config);
    }

    @Override
    public GraphStoreAlgorithmFactory<LinkPredictionPredictPipelineExecutor, LinkPredictionPredictPipelineStreamConfig> algorithmFactory() {
        return new LinkPredictionPredictPipelineAlgorithmFactory<>(this, modelCatalog);
    }

    @Override
    public ComputationResultConsumer<LinkPredictionPredictPipelineExecutor, LinkPredictionResult, LinkPredictionPredictPipelineStreamConfig, Stream<Result>> computationResultConsumer() {
        return (computationResult, executionContext) -> {
            Graph graph = computationResult.graph();

            if (computationResult.isGraphEmpty()) {
                graph.release();
                return Stream.empty();
            }

            return computationResult.result().stream()
                .map(predictedLink -> new Result(
                    graph.toOriginalNodeId(predictedLink.sourceId()),
                    graph.toOriginalNodeId(predictedLink.targetId()),
                    predictedLink.probability()
                ));
        };
    }

    @SuppressWarnings("unused")
    public static final class Result {

        public final long node1;
        public final long node2;
        public final double probability;

        public Result(long node1, long node2, double probability) {
            this.node1 = node1;
            this.node2 = node2;
            this.probability = probability;
        }
    }
}
