/*
 * Copyright (c) 2017-2020 "Neo4j,"
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
package org.neo4j.graphalgo.beta.paths.sourcetarget;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.graphalgo.AlgoBaseProc;
import org.neo4j.graphalgo.GdsCypher;
import org.neo4j.graphalgo.beta.paths.dijkstra.Dijkstra;
import org.neo4j.graphalgo.beta.paths.dijkstra.DijkstraResult;
import org.neo4j.graphalgo.beta.paths.dijkstra.config.ShortestPathDijkstraWriteConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.neo4j.graphalgo.beta.paths.PathTestUtil.WRITE_RELATIONSHIP_TYPE;
import static org.neo4j.graphalgo.beta.paths.PathTestUtil.validationQuery;
import static org.neo4j.graphalgo.config.WriteRelationshipConfig.WRITE_RELATIONSHIP_TYPE_KEY;
import static org.neo4j.graphalgo.utils.StringFormatting.formatWithLocale;

class ShortestPathDijkstraWriteProcTest extends ShortestPathDijkstraProcTest<ShortestPathDijkstraWriteConfig> {

    @Override
    public Class<? extends AlgoBaseProc<Dijkstra, DijkstraResult, ShortestPathDijkstraWriteConfig>> getProcedureClazz() {
        return ShortestPathDijkstraWriteProc.class;
    }

    @Override
    public ShortestPathDijkstraWriteConfig createConfig(CypherMapWrapper mapWrapper) {
        return ShortestPathDijkstraWriteConfig.of("", Optional.empty(), Optional.empty(), mapWrapper);
    }

    @Override
    public CypherMapWrapper createMinimalConfig(CypherMapWrapper mapWrapper) {
        mapWrapper = super.createMinimalConfig(mapWrapper);

        if (!mapWrapper.containsKey(WRITE_RELATIONSHIP_TYPE_KEY)) {
            mapWrapper = mapWrapper.withString(WRITE_RELATIONSHIP_TYPE_KEY, WRITE_RELATIONSHIP_TYPE);
        }

        return mapWrapper;
    }

    @Test
    void testWrite() {
        var relationshipWeightProperty = "cost";
        var graphName = "graph";

        var config = createConfig(createMinimalConfig(CypherMapWrapper.empty()));

        var createQuery = GdsCypher.call()
            .withAnyLabel()
            .withAnyRelationshipType()
            .withRelationshipProperty(relationshipWeightProperty)
            .graphCreate(graphName)
            .yields();
        runQuery(createQuery);

        var query = GdsCypher.call().explicitCreation(graphName)
            .algo("gds.beta.shortestPath.dijkstra")
            .writeMode()
            .addParameter("sourceNode", config.sourceNode())
            .addParameter("targetNode", config.targetNode())
            .addParameter("relationshipWeightProperty", relationshipWeightProperty)
            .addParameter("writeRelationshipType", WRITE_RELATIONSHIP_TYPE)
            .addParameter("writeNodeIds", true)
            .addParameter("writeCosts", true)
            .yields();

        assertCypherResult(query, List.of(Map.of(
            "relationshipsWritten", 1L,
            "createMillis", greaterThan(-1L),
            "computeMillis", greaterThan(-1L),
            "postProcessingMillis", greaterThan(-1L),
            "writeMillis", greaterThan(-1L),
            "configuration", isA(Map.class)
        )));

        assertCypherResult(validationQuery(idA), List.of(Map.of("totalCost", 20.0D, "nodeIds", ids0, "costs", costs0)));
    }

    @ParameterizedTest
    @CsvSource(value = {"true,false", "false,true", "false,false"})
    void testWriteFlags(boolean writeNodeIds, boolean writeCosts) {
        var relationshipWeightProperty = "cost";

        var config = createConfig(createMinimalConfig(CypherMapWrapper.empty()));

        var createQuery = GdsCypher.call()
            .withAnyLabel()
            .withAnyRelationshipType()
            .withRelationshipProperty(relationshipWeightProperty)
            .graphCreate("graph")
            .yields();
        runQuery(createQuery);

        var query = GdsCypher.call().explicitCreation("graph")
            .algo("gds.beta.shortestPath.dijkstra")
            .writeMode()
            .addParameter("sourceNode", config.sourceNode())
            .addParameter("targetNode", config.targetNode())
            .addParameter("relationshipWeightProperty", relationshipWeightProperty)
            .addParameter("writeRelationshipType", WRITE_RELATIONSHIP_TYPE)
            .addParameter("writeNodeIds", writeNodeIds)
            .addParameter("writeCosts", writeCosts)
            .yields();

        runQuery(query);

        var validationQuery = "MATCH ()-[r:%s]->() RETURN r.nodeIds AS nodeIds, r.costs AS costs";
        var rowCount = new MutableInt(0);
        runQueryWithRowConsumer(formatWithLocale(validationQuery, WRITE_RELATIONSHIP_TYPE), row -> {
            rowCount.increment();
            var nodeIds = row.get("nodeIds");
            var costs = row.get("costs");

            if (writeNodeIds) {
                assertNotNull(nodeIds);
            } else {
                assertNull(nodeIds);
            }

            if (writeCosts) {
                assertNotNull(costs);
            } else {
                assertNull(costs);
            }
        });
        assertEquals(1, rowCount.getValue());
    }
}
