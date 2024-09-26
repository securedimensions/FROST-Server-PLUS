/*
 * Copyright (C) 2021-2024 Secure Dimensions GmbH, D-81377
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

import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrincipalFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalFilter.class);

    private final Boolean anonRead;

    public PrincipalFilter(CoreSettings coreSettings) {
        Settings auth = coreSettings.getAuthSettings();
        anonRead = auth.getBoolean(CoreSettings.TAG_AUTH_ALLOW_ANON_READ, true);
        if (anonRead)
            LOGGER.info("Turning on Authentication.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (anonRead && "GET".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
        } else {

            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null) {
                LOGGER.debug("Authorization: " + authHeader);

                String[] authHeaderSplit = authHeader.split("\\s");

                for (int i = 0; i < authHeaderSplit.length; i++) {
                    String token = authHeaderSplit[i];
                    if (token.equalsIgnoreCase("Basic")) {

                        String credentials = new String(Base64.getDecoder().decode(authHeaderSplit[i + 1]));
                        int index = credentials.indexOf(":");
                        if (index != -1) {
                            String username = credentials.substring(0, index).trim();
                            LOGGER.debug("Username: " + username);
                            chain.doFilter(new PrincipalRequestWrapper(username, httpRequest), httpResponse);
                        }
                    }
                }
            } else {
                String realm = "foo bar";
                httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
                httpResponse.sendError(401, "Authentication missing");
            }

        }
    }
}
