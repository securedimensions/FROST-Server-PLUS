/*
 * Copyright (C) 2023 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
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
package de.securedimensions.frostserver.plugin.staplus.test.auth;

import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.AuthProvider;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;
import de.securedimensions.frostserver.plugin.staplus.test.PartyTests;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

public class PrincipalAuthProvider implements AuthProvider, ConfigDefaults {

    public static final String[] HTTP_URL_PATTERNS = {"/v1.0", "/v1.0/*", "/v1.1", "/v1.1/*"};

    private CoreSettings coreSettings;

    @Override
    public void init(CoreSettings coreSettings) {
        this.coreSettings = coreSettings;
    }

    @Override
    public String checkForUpgrades() {
        return "";
    }

    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
        return true;
    }

    @Override
    public void addFilter(Object context, CoreSettings coreSettings) {
        if (!(context instanceof ServletContext)) {
            throw new IllegalArgumentException("Context must be a ServletContext to add Filters.");
        }
        ServletContext servletContext = (ServletContext) context;
        Filter auth = new PrincipalFilter(coreSettings);
        FilterRegistration.Dynamic principalFilterSta = servletContext.addFilter("PrincipalFilterSta", auth);

        String[] urlPatterns = Arrays.copyOf(HTTP_URL_PATTERNS, HTTP_URL_PATTERNS.length);
        principalFilterSta.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), true, urlPatterns);
    }

    @Override
    public boolean isValidUser(String clientId, String username, String password) {

        return false;
    }

    @Override
    public boolean userHasRole(String clientId, String userName, String roleName) {

        if (userName.equalsIgnoreCase(PartyTests.ADMIN))
            return true;
        else
            return false;
    }

}
