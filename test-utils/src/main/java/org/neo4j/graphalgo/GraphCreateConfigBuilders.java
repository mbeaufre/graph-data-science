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

package org.neo4j.graphalgo;

import org.immutables.builder.Builder;
import org.immutables.value.Value;
import org.neo4j.graphalgo.config.AlgoBaseConfig;
import org.neo4j.graphalgo.config.GraphCreateFromCypherConfig;
import org.neo4j.graphalgo.config.GraphCreateFromStoreConfig;
import org.neo4j.graphalgo.config.ImmutableGraphCreateFromCypherConfig;
import org.neo4j.graphalgo.config.ImmutableGraphCreateFromStoreConfig;
import org.neo4j.graphalgo.core.Aggregation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.neo4j.graphalgo.NodeLabel.ALL_NODES;
import static org.neo4j.graphalgo.RelationshipType.ALL_RELATIONSHIPS;
import static org.neo4j.graphalgo.config.GraphCreateFromCypherConfig.ALL_NODES_QUERY;
import static org.neo4j.graphalgo.config.GraphCreateFromCypherConfig.ALL_RELATIONSHIPS_QUERY;

@Value.Style(builderVisibility = Value.Style.BuilderVisibility.PUBLIC, depluralize = true, deepImmutablesDetection = true)
final class GraphCreateConfigBuilders {

    private GraphCreateConfigBuilders() { }

    /**
     * Factory method that defines the generation of {@link StoreConfigBuilder}.
     */
    @Builder.Factory
    static GraphCreateFromStoreConfig storeConfig(
        Optional<String> userName,
        Optional<String> graphName,
        List<String> nodeLabels,
        List<String> relationshipTypes,
        List<NodeProjection> nodeProjections,
        List<RelationshipProjection> relationshipProjections,
        Map<String, NodeProjection> nodeProjectionsWithIdentifier,
        Map<String, RelationshipProjection> relationshipProjectionsWithIdentifier,
        List<PropertyMapping> nodeProperties,
        List<PropertyMapping> relationshipProperties,
        Optional<Integer> concurrency,
        @Builder.Switch(defaultName = "PROJECTION") AnyLabel anyLabel,
        @Builder.Switch(defaultName = "PROJECTION") AnyRelationshipType anyRelationshipType,
        Optional<Orientation> globalProjection,
        Optional<Aggregation> globalAggregation,
        Optional<Boolean> validateRelationships
    ) {
        // Node projections
        Map<String, NodeProjection> tempNP = new LinkedHashMap<>();
        nodeLabels.forEach(label -> tempNP.put(label, NodeProjection.of(label, PropertyMappings.of())));
        nodeProjections.forEach(np -> tempNP.put(np.label(), np));
        nodeProjectionsWithIdentifier.forEach(tempNP::put);

        if (tempNP.isEmpty() && anyLabel == AnyLabel.LOAD) {
            tempNP.put(ALL_NODES.name, NodeProjection.all());
        }

        // Relationship projections
        Map<String, RelationshipProjection> tempRP = new LinkedHashMap<>();
        Orientation orientation = globalProjection.orElse(Orientation.NATURAL);
        Aggregation aggregation = globalAggregation.orElse(Aggregation.DEFAULT);

        relationshipTypes.forEach(relType -> tempRP.put(
            relType,
            RelationshipProjection.of(relType, orientation, aggregation)
        ));
        relationshipProjections.forEach(rp -> tempRP.put(rp.type(), rp));
        relationshipProjectionsWithIdentifier.forEach(tempRP::put);

        if (tempRP.isEmpty() && anyRelationshipType == AnyRelationshipType.LOAD) {
            tempRP.put(ALL_RELATIONSHIPS.name, RelationshipProjection.builder()
                .type("*")
                .orientation(orientation)
                .aggregation(aggregation)
                .build());
        }

        PropertyMappings relationshipPropertyMappings = PropertyMappings.builder()
            .addAllMappings(relationshipProperties)
            .withDefaultAggregation(aggregation)
            .build();

        NodeProjections np = NodeProjections.of(tempNP.entrySet().stream().collect(Collectors.toMap(
            e -> NodeLabel.of(e.getKey()),
            Map.Entry::getValue
        )));

        RelationshipProjections rp = RelationshipProjections.of(tempRP.entrySet().stream().collect(Collectors.toMap(
            e -> RelationshipType.of(e.getKey()),
            Map.Entry::getValue
        )));

        return ImmutableGraphCreateFromStoreConfig.builder()
            .username(userName.orElse(""))
            .graphName(graphName.orElse(""))
            .nodeProjections(np)
            .relationshipProjections(rp)
            .nodeProperties(PropertyMappings.of(nodeProperties))
            .relationshipProperties(relationshipPropertyMappings)
            .readConcurrency(concurrency.orElse(AlgoBaseConfig.DEFAULT_CONCURRENCY))
            .validateRelationships(validateRelationships.orElse(false))
            .build()
            .withNormalizedPropertyMappings();
    }

    /**
     * Factory method that defines the generation of {@link CypherConfigBuilder}.
     */
    @Builder.Factory
    static GraphCreateFromCypherConfig cypherConfig(
        Optional<String> userName,
        Optional<String> graphName,
        Optional<String> nodeQuery,
        Optional<String> relationshipQuery,
        @Builder.Switch(defaultName = "PROJECTION") AnyLabel anyLabel,
        @Builder.Switch(defaultName = "PROJECTION") AnyRelationshipType anyRelationshipType,
        Optional<Integer> concurrency,
        Optional<Boolean> validateRelationships,
        Optional<Map<String, Object>> parameters
    ) {
        if (!(nodeQuery.isPresent() || anyLabel == AnyLabel.LOAD)) {
            throw new IllegalArgumentException("Missing nodeQuery or loadAnyLabel().");
        }

        if (!(relationshipQuery.isPresent() || anyRelationshipType == AnyRelationshipType.LOAD)) {
            throw new IllegalArgumentException("Missing relationshipQuery or loadAnyRelationshipType().");
        }

        RelationshipProjections relationshipProjections = RelationshipProjections.empty();

        return ImmutableGraphCreateFromCypherConfig.builder()
            .username(userName.orElse(""))
            .graphName(graphName.orElse(""))
            .nodeQuery(nodeQuery.orElse(ALL_NODES_QUERY))
            .relationshipQuery(relationshipQuery.orElse(ALL_RELATIONSHIPS_QUERY))
            .relationshipProjections(relationshipProjections)
            .readConcurrency(concurrency.orElse(AlgoBaseConfig.DEFAULT_CONCURRENCY))
            .validateRelationships(validateRelationships.orElse(true))
            .parameters(parameters.orElse(Collections.emptyMap()))
            .build();
    }

    enum AnyLabel {
        PROJECTION, LOAD
    }

    enum AnyRelationshipType {
        PROJECTION, LOAD
    }
}
