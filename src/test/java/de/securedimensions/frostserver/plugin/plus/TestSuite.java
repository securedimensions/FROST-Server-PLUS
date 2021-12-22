/*
 * Copyright (C) 2021 Secure Dimensions GmbH, D-81377
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
package de.securedimensions.frostserver.plugin.plus;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

/**
 * TestSuite for the executing all tests regarding the ownership concept. 
 *
 * @author Andreas Matheus
 */
@RunWith(Suite.class)
@SuiteClasses({PartyTests.class, DatastreamTests.class, MultiDatastreamTests.class, GroupTests.class, ThingTests.class, ObservationTests.class, LicenseTests.class})
public class TestSuite {

    private static final long serialVersionUID = 1639739965;

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSuite.class);

	public TestSuite()
	{
		LOGGER.info("Running all tests for the PLUS plugin");
	}

}
