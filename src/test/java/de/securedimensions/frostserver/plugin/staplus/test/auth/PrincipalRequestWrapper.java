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
package de.securedimensions.frostserver.plugin.staplus.test.auth;

import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.securedimensions.frostserver.plugin.staplus.test.PartyTests;
import java.security.Principal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class PrincipalRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

    String user;
    List<String> roles = null;

    public PrincipalRequestWrapper(String User, HttpServletRequest request) {
        super(request);
        this.user = User;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (role.equalsIgnoreCase(PartyTests.ADMIN) && user.equalsIgnoreCase(PartyTests.ADMIN))
            return true;

        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        if (this.user == null) {
            return null;
        }

        if (user.equalsIgnoreCase(PartyTests.ADMIN)) {
            // Admin principal 
            return new PrincipalExtended(user, true);

        } else {
            // Principal that returns our user
            return new Principal() {

                public String getName() {
                    return user;
                }
            };
        }
    }
}
