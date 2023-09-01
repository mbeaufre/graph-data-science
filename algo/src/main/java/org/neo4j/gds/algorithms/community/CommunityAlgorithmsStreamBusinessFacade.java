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
package org.neo4j.gds.algorithms.community;

import org.neo4j.gds.algorithms.AlgorithmComputationResult;
import org.neo4j.gds.algorithms.StreamComputationResult;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.User;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.gds.kcore.KCoreDecompositionBaseConfig;
import org.neo4j.gds.kcore.KCoreDecompositionResult;
import org.neo4j.gds.louvain.LouvainBaseConfig;
import org.neo4j.gds.louvain.LouvainResult;
import org.neo4j.gds.wcc.WccBaseConfig;

public class CommunityAlgorithmsStreamBusinessFacade {
    private final CommunityAlgorithmsFacade communityAlgorithmsFacade;

    public CommunityAlgorithmsStreamBusinessFacade(CommunityAlgorithmsFacade communityAlgorithmsFacade) {
        this.communityAlgorithmsFacade = communityAlgorithmsFacade;
    }

    public StreamComputationResult<WccBaseConfig, DisjointSetStruct> streamWcc(
        String graphName,
        WccBaseConfig config,
        User user,
        DatabaseId databaseId
    ) {

        var result = this.communityAlgorithmsFacade.wcc(
            graphName,
            config,
            user,
            databaseId
        );

        return createStreamComputationResult(result);
    }


    public StreamComputationResult<KCoreDecompositionBaseConfig, KCoreDecompositionResult> streamKCore(
        String graphName,
        KCoreDecompositionBaseConfig config,
        User user,
        DatabaseId databaseId
    ) {

        var result = this.communityAlgorithmsFacade.kCore(
            graphName,
            config,
            user,
            databaseId
        );
        
        return createStreamComputationResult(result);

    }

    public StreamComputationResult<LouvainBaseConfig, LouvainResult> streamLouvain(
        String graphName,
        LouvainBaseConfig config,
        User user,
        DatabaseId databaseId
    ) {

        var result = this.communityAlgorithmsFacade.louvain(
            graphName,
            config,
            user,
            databaseId
        );

        return createStreamComputationResult(result);
    }

    private <C extends AlgoBaseConfig, RESULT> StreamComputationResult<C, RESULT> createStreamComputationResult(
        AlgorithmComputationResult<C, RESULT> result
    ) {

        return StreamComputationResult.of(
            result.result(),
            result.configuration(),
            result.graph(),
            result.graphStore()
        );

    }

}
