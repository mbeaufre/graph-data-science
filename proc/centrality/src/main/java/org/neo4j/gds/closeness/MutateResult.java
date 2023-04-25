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
package org.neo4j.gds.closeness;

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.result.AbstractCentralityResultBuilder;
import org.neo4j.gds.results.StandardMutateResult;

import java.util.Map;

@SuppressWarnings("unused")
public final class MutateResult extends StandardMutateResult {

    public final long nodePropertiesWritten;
    public final String mutateProperty;
    public final Map<String, Object> centralityDistribution;

    MutateResult(
        long nodePropertiesWritten,
        long preProcessingMillis,
        long computeMillis,
        long postProcessingMillis,
        long mutateMillis,
        String mutateProperty,
        @Nullable Map<String, Object> centralityDistribution,
        Map<String, Object> config
    ) {
        super(preProcessingMillis, computeMillis, postProcessingMillis, mutateMillis, config);
        this.mutateProperty = mutateProperty;
        this.centralityDistribution = centralityDistribution;
        this.nodePropertiesWritten = nodePropertiesWritten;
    }

    static final class Builder extends AbstractCentralityResultBuilder<MutateResult> {
        public String mutateProperty;

         Builder(ProcedureReturnColumns returnColumns, int concurrency) {
            super(returnColumns, concurrency);
        }

        public Builder withMutateProperty(String mutateProperty) {
            this.mutateProperty = mutateProperty;
            return this;
        }

        @Override
        public MutateResult buildResult() {
            return new MutateResult(
                nodePropertiesWritten,
                preProcessingMillis,
                computeMillis,
                postProcessingMillis,
                mutateMillis,
                mutateProperty,
                centralityHistogram,
                config.toMap()
            );
        }
    }
}