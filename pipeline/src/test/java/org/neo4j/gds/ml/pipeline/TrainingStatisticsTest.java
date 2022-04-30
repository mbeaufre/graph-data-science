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
package org.neo4j.gds.ml.pipeline;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.ml.metrics.ModelStats;
import org.neo4j.gds.ml.metrics.CandidateStats;
import org.neo4j.gds.ml.metrics.Metric;
import org.neo4j.gds.ml.models.TrainerConfig;
import org.neo4j.gds.ml.models.TrainingMethod;
import org.neo4j.gds.ml.models.logisticregression.LogisticRegressionTrainConfig;
import org.neo4j.gds.ml.models.randomforest.RandomForestClassifierTrainerConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.ml.metrics.LinkCrossValidationMetric.AUCPR;
import static org.neo4j.gds.ml.metrics.classification.AllClassMetric.ACCURACY;
import static org.neo4j.gds.ml.metrics.classification.AllClassMetric.F1_WEIGHTED;
import static org.neo4j.gds.ml.metrics.classification.OutOfBagError.OUT_OF_BAG_ERROR;
import static org.neo4j.gds.ml.metrics.regression.RegressionMetrics.ROOT_MEAN_SQUARED_ERROR;

class TrainingStatisticsTest {

    public static Stream<Arguments> mainMetricWithExpectecWinner() {
        return Stream.of(
            Arguments.of(List.of(ROOT_MEAN_SQUARED_ERROR, AUCPR), "lower average rmse"),
            Arguments.of(List.of(AUCPR, ROOT_MEAN_SQUARED_ERROR), "higher average aucpr")
        );
    }

    @ParameterizedTest
    @MethodSource("mainMetricWithExpectecWinner")
    void selectsBestParametersAccordingToMainMetric(List<Metric> metrics, String expectedWinner) {
        var trainingStatistics = new TrainingStatistics(metrics);

        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("lower average rmse"),
            Map.of(),
            Map.of(
                ROOT_MEAN_SQUARED_ERROR, ModelStats.of(
                    0.2,
                    0.2,
                    0.2
                ),
                AUCPR, ModelStats.of(
                    0.0,
                    1000,
                    1000
                )
            )
        ));
        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("higher average aucpr"),
            Map.of(),
            Map.of(
                ROOT_MEAN_SQUARED_ERROR, ModelStats.of(
                    0.3,
                    0.1,
                    0.1
                ),
                AUCPR, ModelStats.of(
                    0.4,
                    0.2,
                    0.2
                )
            )
        ));

        assertThat(((TestTrainerConfig) trainingStatistics.bestParameters()).name).isEqualTo(expectedWinner);
    }

    @Test
    void getBestTrialStuff() {
        var trainingStatistics = new TrainingStatistics(List.of(AUCPR, F1_WEIGHTED));

        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("bad"),
            Map.of(),
            Map.of(
                AUCPR,
                ModelStats.of(
                    0.1,
                    1000,
                    1000
                )
            )
        ));
        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("better"),
            Map.of(),
            Map.of(
                AUCPR,
                ModelStats.of(
                    0.2,
                    0.2,
                    0.2
                )
            )
        ));
        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("same as better"),
            Map.of(),
            Map.of(
                AUCPR,
                ModelStats.of(
                    0.2,
                    0.2,
                    0.2
                )
            )
        ));
        trainingStatistics.addCandidateStats(CandidateStats.of(
            new TestTrainerConfig("notprimarymetric"),
            Map.of(),
            Map.of(
                AUCPR, ModelStats.of(
                    0.0,
                    0.0,
                    0.0
                ),
                F1_WEIGHTED, ModelStats.of(
                    5000,
                    5000,
                    5000
                )
            )
        ));

        assertThat(trainingStatistics.getBestTrialScore()).isCloseTo(0.2, Offset.offset(0.001));
        assertThat(trainingStatistics.getBestTrialIdx()).isEqualTo(1);
    }

    @Test
    void rendersBestModel() {
        var trainingStatistics = new TrainingStatistics(List.of(AUCPR, F1_WEIGHTED, OUT_OF_BAG_ERROR));

        var candidate = new TestTrainerConfig("train");
        ModelStats trainStats = ModelStats.of(
            0.1,
            0.1,
            0.1
        );
        ModelStats validationStats = ModelStats.of(
            0.4,
            0.3,
            0.5
        );
        ModelStats oobStats = ModelStats.of(
            0.5,
            0.4,
            0.9
        );
        trainingStatistics.addCandidateStats(CandidateStats.of(
            candidate,
            Map.of(
                AUCPR, trainStats,
                F1_WEIGHTED, trainStats
            ),
            Map.of(
                AUCPR, validationStats,
                F1_WEIGHTED, validationStats,
                OUT_OF_BAG_ERROR, oobStats
            )
        ));
        trainingStatistics.addTestScore(AUCPR, 1);
        trainingStatistics.addTestScore(F1_WEIGHTED, 2);
        trainingStatistics.addOuterTrainScore(AUCPR, 3);
        trainingStatistics.addOuterTrainScore(F1_WEIGHTED, 4);

        var winningModelMetrics = trainingStatistics.bestCandidate().toMap();

        assertThat(winningModelMetrics).isEqualTo(
            Map.of(
                "metrics", Map.of(
                    "AUCPR", Map.of(
                        "train", Map.of("avg", 0.1, "max", 0.1, "min", 0.1),
                        "validation", Map.of("avg", 0.4, "max", 0.5, "min", 0.3)
                    ),
                    "F1_WEIGHTED", Map.of(
                        "train", Map.of("avg", 0.1, "max", 0.1, "min", 0.1),
                        "validation", Map.of("avg", 0.4, "max", 0.5, "min", 0.3)
                    ),
                    "OUT_OF_BAG_ERROR", Map.of(
                        "validation", Map.of("avg", 0.5, "max", 0.9, "min", 0.4)
                    )
                ),
                "parameters", Map.of("methodName", "RandomForest", "name", "train")
            )
        );

        assertThat(trainingStatistics.winningModelTestMetrics()).isEqualTo(
            Map.of(
                AUCPR, 1.0,
                F1_WEIGHTED, 2.0
            )
        );
        assertThat(trainingStatistics.winningModelOuterTrainMetrics()).isEqualTo(
            Map.of(
                AUCPR, 3.0,
                F1_WEIGHTED, 4.0
            )
        );
    }

    @Test
    void toMap() {
        RandomForestClassifierTrainerConfig firstCandidate = RandomForestClassifierTrainerConfig.DEFAULT;
        LogisticRegressionTrainConfig secondCandidate = LogisticRegressionTrainConfig.DEFAULT;

        var selectResult = new TrainingStatistics(List.of(ACCURACY));

        selectResult.addCandidateStats(CandidateStats.of(
            firstCandidate,
            Map.of(ACCURACY, ModelStats.of(0.33, 0.1, 0.6)),
            Map.of(ACCURACY, ModelStats.of(0.4, 0.3, 0.5))
        ));
        selectResult.addCandidateStats(CandidateStats.of(
            secondCandidate,
            Map.of(ACCURACY, ModelStats.of(0.2, 0.01, 0.7)),
            Map.of(ACCURACY, ModelStats.of(0.8, 0.7, 0.9))
        ));

        var expectedTrainAccuracyStats1 = Map.of("avg", 0.33, "min", 0.1, "max", 0.6);
        var expectedTrainAccuracyStats2 = Map.of("avg", 0.2, "min", 0.01, "max", 0.7);

        var expectedValidationAccuracyStats1 = Map.of("avg", 0.4, "min", 0.3, "max", 0.5);
        var expectedValidationAccuracyStats2 = Map.of("avg", 0.8, "min", 0.7, "max", 0.9);

        var mapResult = selectResult.toMap();
        assertThat(mapResult)
            .containsEntry("bestParameters", secondCandidate.toMap())
            .containsKey("modelCandidates");
        assertThat((List) mapResult.get("modelCandidates"))
            .containsExactlyInAnyOrder(
                Map.of(
                    "metrics", Map.of(
                        ACCURACY.name(), Map.of(
                            "train", expectedTrainAccuracyStats1,
                            "validation", expectedValidationAccuracyStats1
                        )
                    ),
                    "parameters", firstCandidate.toMapWithTrainerMethod()
                ),
                Map.of(
                    "metrics", Map.of(
                        ACCURACY.name(), Map.of(
                            "train", expectedTrainAccuracyStats2,
                            "validation", expectedValidationAccuracyStats2
                        )
                    ),
                    "parameters", secondCandidate.toMapWithTrainerMethod()
                )
            );
    }

    private static final class TestTrainerConfig implements TrainerConfig {

        private final String name;

        private TestTrainerConfig(String name) {this.name = name;}

        @Override
        public Map<String, Object> toMap() {
            return Map.of("name", name);
        }

        @Override
        public TrainingMethod method() {
            return TrainingMethod.RandomForestClassification;
        }
    }

}
