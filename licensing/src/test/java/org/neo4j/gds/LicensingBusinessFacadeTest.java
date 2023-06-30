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
package org.neo4j.gds;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class LicensingBusinessFacadeTest {

    @Test
    void test() {
        LicensingService licensingServiceMock = mock(LicensingService.class);
        var licensingBusinessFacade = new LicensingBusinessFacade(licensingServiceMock);

        when(licensingServiceMock.get()).thenReturn(new TestLicenseStates.Unlicensed());
        LicenseDetails licenseDetails = licensingBusinessFacade.licenseDetails();

        assertThat(licenseDetails.isLicensed()).isFalse();
        assertThat(licenseDetails.details()).isEqualTo("No valid GDS license specified.");
    }

}
