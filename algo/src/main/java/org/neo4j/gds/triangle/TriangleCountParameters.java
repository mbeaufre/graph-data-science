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

import org.neo4j.gds.annotation.Parameters;

@Parameters
public final class TriangleCountParameters {

    public static TriangleCountParameters create(int concurrency, long maxDegree) {
        return new TriangleCountParameters(concurrency, maxDegree);
    }
    private final int concurrency;
    private final long maxDegree;

    private TriangleCountParameters(int concurrency, long maxDegree) {
        this.concurrency = concurrency;
        this.maxDegree = maxDegree;
    }

    public int concurrency() {
        return concurrency;
    }

    public long maxDegree() {
        return maxDegree;
    }
}
