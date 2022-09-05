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
package org.neo4j.gds.modularity;

import com.carrotsearch.hppc.BitSet;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import static org.assertj.core.api.Assertions.assertThat;


class ModularityColorArrayTest {

    @Test
    void shouldComputeCorrectly() {
        HugeLongArray colors = HugeLongArray.of(0, 1, 2, 2, 1, 0);
        BitSet usedColors = new BitSet(6);
        usedColors.set(0, 3);


        ModularityColorArray modularityColorArray = ModularityColorArray.createModularityColorArray(
            colors,
            6,
            usedColors
        );

        assertThat(modularityColorArray.getNumberOfColors()).isEqualTo(3);
        assertThat(modularityColorArray.getCount(0)).isEqualTo(2);
        assertThat(modularityColorArray.getCount(2)).isEqualTo(2);
        assertThat(modularityColorArray.getCount(4)).isEqualTo(2);

        assertThat(colors.get(modularityColorArray.get(0))).isEqualTo(colors.get(modularityColorArray.get(1)));
        assertThat(colors.get(modularityColorArray.get(2))).isEqualTo(colors.get(modularityColorArray.get(3)));
        assertThat(colors.get(modularityColorArray.get(4))).isEqualTo(colors.get(modularityColorArray.get(5)));

        assertThat(modularityColorArray.getNextStartingCoordinate(0)).isEqualTo(2);
        assertThat(modularityColorArray.getNextStartingCoordinate(2)).isEqualTo(4);
        assertThat(modularityColorArray.getNextStartingCoordinate(4)).isEqualTo(6);


    }
}
