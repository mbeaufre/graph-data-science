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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.gds.catalog.GraphCreateProc;
import org.neo4j.gds.catalog.GraphStreamNodePropertiesProc;
import org.neo4j.gds.ml.core.functions.Weights;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.linkmodels.PredictedLink;
import org.neo4j.gds.ml.linkmodels.pipeline.FeaturePipeline;
import org.neo4j.gds.ml.linkmodels.pipeline.ProcedureTestUtils;
import org.neo4j.gds.ml.linkmodels.pipeline.linkFeatures.LinkFeatureStepFactory;
import org.neo4j.gds.ml.linkmodels.pipeline.logisticRegression.ImmutableLinkLogisticRegressionData;
import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.graphalgo.BaseProcTest;
import org.neo4j.graphalgo.GdsCypher;
import org.neo4j.graphalgo.NodeLabel;
import org.neo4j.graphalgo.Orientation;
import org.neo4j.graphalgo.RelationshipType;
import org.neo4j.graphalgo.api.DefaultValue;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.core.loading.GraphStoreCatalog;
import org.neo4j.graphalgo.core.utils.progress.v2.tasks.ProgressTracker;
import org.neo4j.graphalgo.extension.Neo4jGraph;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LinkPredictionTest extends BaseProcTest {
    @Neo4jGraph
    static String GDL = "CREATE " +
                        "  (n0:N {a: 1.0, b: 0.8, c: 1.0})" +
                        ", (n1:N {a: 2.0, b: 1.0, c: 1.0})" +
                        ", (n2:N {a: 3.0, b: 1.5, c: 1.0})" +
                        ", (n3:N {a: 0.0, b: 2.8, c: 1.0})" +
                        ", (n4:N {a: 1.0, b: 0.9, c: 1.0})" +
                        ", (n1)-[:T]->(n2)" +
                        ", (n3)-[:T]->(n4)" +
                        ", (n1)-[:T]->(n3)" +
                        ", (n2)-[:T]->(n4)";

    private static final double[] WEIGHTS = new double[]{-2.0, -1.0, 3.0};

    private Graph graph;

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            GraphCreateProc.class,
            GraphStreamNodePropertiesProc.class
        );
        String createQuery = GdsCypher.call()
            .withNodeLabel("N")
            .withRelationshipType("T", Orientation.UNDIRECTED)
            .withNodeProperties(List.of("a", "b", "c"), DefaultValue.DEFAULT)
            .graphCreate("g")
            .yields();

        runQuery(createQuery);

        graph = GraphStoreCatalog.get(getUsername(), db.databaseId(), "g").graphStore().getUnion();
    }

    @ParameterizedTest
    @CsvSource(value = {"3, 1", "3, 4", "50, 1", "50, 4"})
    void shouldPredictWithTopN(int topN, int concurrency) {
        ProcedureTestUtils.applyOnProcedure(db, (Consumer<? super AlgoBaseProc<?, ?, ?>>) caller -> {
            var featurePipeline = new FeaturePipeline(caller, db.databaseId(), getUsername());
            featurePipeline.addFeature(
                LinkFeatureStepFactory.L2.name(),
                Map.of("nodeProperties", List.of("a", "b", "c"))
            );
            var modelData = ImmutableLinkLogisticRegressionData.of(new Weights<>(new Matrix(
                WEIGHTS,
                1,
                WEIGHTS.length
            )));
            var linkPrediction = new LinkPrediction(
                modelData,
                featurePipeline,
                "g",
                List.of(NodeLabel.of("N")),
                List.of(RelationshipType.of("T")),
                graph,
                concurrency,
                topN,
                0D,
                ProgressTracker.NULL_TRACKER
            );
            var predictionResult = linkPrediction.compute();
            var predictedLinks = predictionResult.stream().collect(Collectors.toList());
            assertThat(predictedLinks).hasSize(Math.min(topN, 6));
            var expectedLinks = List.of(
                PredictedLink.of(0, 4, 0.49750002083312506),
                PredictedLink.of(1, 4, 0.11815697780926958),
                PredictedLink.of(0, 1, 0.11506673204554983),
                PredictedLink.of(0, 3, 0.0024726231566347774),
                PredictedLink.of(0, 2, 2.0547103309367397E-4),
                PredictedLink.of(2, 3, 2.810228605019867E-9)
            );
            assertThat(expectedLinks).containsAll(predictedLinks);
        });
    }
}
