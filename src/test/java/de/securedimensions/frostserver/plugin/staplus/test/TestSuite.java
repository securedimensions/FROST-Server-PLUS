/*
 * Copyright (C) 2021-2023 Secure Dimensions GmbH, D-81377
 * Munich, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.securedimensions.frostserver.plugin.staplus.test;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestSuite for the executing all tests regarding the ownership concept.
 *
 * @author Andreas Matheus
 */
@Suite
@SelectClasses({
    PartyTests.Imp10Tests.class,
    PartyTests.Imp11Tests.class,
    DatastreamTests.Imp10Tests.class,
    DatastreamTests.Imp11Tests.class,
    MultiDatastreamTests.Imp10Tests.class,
    MultiDatastreamTests.Imp11Tests.class,
    GroupTests.Imp10Tests.class,
    GroupTests.Imp11Tests.class,
    ProjectTests.Imp10Tests.class,
    ProjectTests.Imp11Tests.class,
    ThingTests.Imp10Tests.class,
    ThingTests.Imp11Tests.class,
    ObservationTests.Imp10Tests.class,
    ObservationTests.Imp11Tests.class,
    LicenseTests.Imp10Tests.class,
    LicenseTests.Imp11Tests.class,
    RelationTests.Imp10Tests.class,
    RelationTests.Imp11Tests.class
})
public class TestSuite {

    private static final long serialVersionUID = 1639739965;

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSuite.class);

    public TestSuite() {
        LOGGER.info("Running all tests for the STAplus plugin");
    }

}
