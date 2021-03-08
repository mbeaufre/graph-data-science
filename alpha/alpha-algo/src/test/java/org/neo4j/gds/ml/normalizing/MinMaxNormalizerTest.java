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
package org.neo4j.gds.ml.normalizing;

import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.api.nodeproperties.DoubleNodeProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MinMaxNormalizerTest {

    @Test
    void normalizes() {
        var properties = (DoubleNodeProperties) nodeId -> nodeId;
        var minMaxNormalizer = MinMaxNormalizer.create(properties, 10);

        assertThat(minMaxNormalizer.min).isEqualTo(0D);
        assertThat(minMaxNormalizer.max).isEqualTo(9D);

        for (int i = 0; i < 10; i++) {
            assertThat(minMaxNormalizer.normalize(i)).isEqualTo(i / 9D);
        }
    }

    @Test
    void avoidsDivByZero() {
        var properties = (DoubleNodeProperties) nodeId -> 4D;
        var minMaxNormalizer = MinMaxNormalizer.create(properties, 10);

        assertThat(minMaxNormalizer.min).isEqualTo(4D);
        assertThat(minMaxNormalizer.max).isEqualTo(4D);

        for (int i = 0; i < 10; i++) {
            assertThat(minMaxNormalizer.normalize(i)).isEqualTo(0D);
        }
    }

}
