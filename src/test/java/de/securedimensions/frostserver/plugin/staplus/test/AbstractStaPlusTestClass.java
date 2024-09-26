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
package de.securedimensions.frostserver.plugin.staplus.test;

import static de.fraunhofer.iosb.ilt.statests.util.EntityUtils.deleteAll;

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.exception.NotFoundException;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsPlus;
import de.fraunhofer.iosb.ilt.frostclient.utils.TokenManager;
import de.fraunhofer.iosb.ilt.statests.AbstractTestClass;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.fraunhofer.iosb.ilt.statests.util.EntityUtils;
import java.util.Base64;
import java.util.Map;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Common STA plus test stuff.
 */
public abstract class AbstractStaPlusTestClass extends AbstractTestClass {

    public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
    public static final String LJS = "21232f29-7a57-35a7-8389-4a0e4a801fc3";
    public static final String JB = "my name is Bond - James Bond";
    public static final String ADMIN = "admin";

    protected static SensorThingsPlus pMdl;
    protected static SensorThingsService serviceSTAplus;

    public AbstractStaPlusTestClass(ServerVersion serverVersion) {
        super(serverVersion);
    }

    public AbstractStaPlusTestClass(ServerVersion serverVersion, Map<String, String> properties) {
        super(serverVersion, properties);
    }

    public static void cleanup() throws ServiceFailureException {
        setAuth(serviceSTAplus, ADMIN, "");
        deleteAll(serviceSTAplus);
    }

    public static void deleteAll(SensorThingsService service) throws ServiceFailureException {
        ModelRegistry mr = service.getModelRegistry();
        if (mr.getEntityTypeForName("Thing") != null) {
            // First delete Things, for efficiency
            EntityUtils.deleteAll(service.dao(mr.getEntityTypeForName("Thing")));
        }
        for (de.fraunhofer.iosb.ilt.frostclient.model.EntityType et : mr.getEntityTypes()) {
            if ("Thing".equals(et.entityName)) {
                continue;
            }
            if ("License".equals(et.entityName)) {
                continue;
            }
            try {
                EntityUtils.deleteAll(service.dao(et));
            } catch (NotFoundException exc) {
                // the model has entity types that dont exist on the server.
            }
        }
    }

    public static void setAuth(HttpRequestBase http, String username, String password) {
        String credentials = username + ":" + password;
        String base64 = Base64.getEncoder().encodeToString(credentials.getBytes());
        http.setHeader("Authorization", "BASIC " + base64);
    }

    public static void setAuth(SensorThingsService service, String username, String password) {
        final String credentials = username + ":" + password;
        final String base64 = Base64.getEncoder().encodeToString(credentials.getBytes());
        final String authHeaderValue = "BASIC " + base64;
        service.setTokenManager(new TokenManager() {
            @Override
            public void addAuthHeader(HttpRequest hr) {
                hr.addHeader("Authorization", authHeaderValue);
            }

            @Override
            public TokenManager setHttpClient(CloseableHttpClient chc) {
                // We don't need a HTTPClient.
                return this;
            }

            @Override
            public CloseableHttpClient getHttpClient() {
                // We don't need a HTTPClient.
                return null;
            }
        });
    }
}
