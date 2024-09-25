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

import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsPlus;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsSensingV11;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import de.securedimensions.frostserver.plugin.staplus.test.auth.PrincipalAuthProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the Party class properties. According to the ownership concept, a
 * Party's properties can only be changed by the user that 'owns' the Party
 * instance. That user has the same UUID as the Party's authId property.
 *
 * @author Andreas Matheus
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class PartyTests extends AbstractStaPlusTestClass {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyTests.class);
    private static final long serialVersionUID = 1639739965;
    private static final String USER_SHOULD_BE_ABLE_TO_CREATE = "A user should be able to create without setting `authId`.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE = "Admin should be able to create.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE = "Admin should be able to update.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE = "Admin should be able to delete.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE = "Same user should be able to create.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE = "Same user should be able to update.";
    private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID = "Same user should NOT be able to update authId.";
    private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Same User should NOT be able to delete.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE = "Other user should NOT be able to create.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE = "Other user should NOT be able to update.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Other user should NOT be able to delete.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE = "anon should NOT be able to create.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE = "anon should NOT be able to update.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE = "anon should NOT be able to delete.";
    private static final int HTTP_CODE_200 = 200;
    private static final int HTTP_CODE_201 = 201;
    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_401 = 401;
    private static final int HTTP_CODE_403 = 403;
    private static final String PARTY = "{\"description\": \"\", \"displayName\": \"me\", \"role\": \"individual\"}";
    private static final String PARTY_ALICE = String.format("{\"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"displayName\": \"Alice in Wonderland\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
    private static final String PARTY_LJS = String.format("{\"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"displayName\": \"Long John Silver Citizen Scientist\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);
    private static final String PARTY_JB = String.format("{\"description\": \"A character created by the British journalist and novelist Ian Fleming\", \"displayName\": \"James Bond\", \"role\": \"individual\", \"authId\": \"%s\"}", JB);

    private static final Map<String, String> SERVER_PROPERTIES = new LinkedHashMap<>();

    static {
        SERVER_PROPERTIES.put("plugins.plugins", PluginPLUS.class.getName());
        SERVER_PROPERTIES.put("plugins.staplus.enable", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceOwnership", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceLicensing", "false");
        SERVER_PROPERTIES.put("plugins.staplus.idType.license", "String");
        SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
        SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entity Type
        SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
        SERVER_PROPERTIES.put("plugins.coreModel.idType", "LONG");
        SERVER_PROPERTIES.put("plugins.multiDatastream.enable", "true");
    }

    //private static SensorThingsService service;
    private final String partyLJSUrl;
    private final String partyALICEUrl;
    private final String partyJBUrl;

    public PartyTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
        // This is the party that we are going to apply the Update and Delete tests
        // on...
        partyLJSUrl = serverSettings.getServiceUrl(version) + "/Parties('" + LJS + "')";
        partyALICEUrl = serverSettings.getServiceUrl(version) + "/Parties('" + ALICE + "')";
        partyJBUrl = serverSettings.getServiceUrl(version) + "/Parties('" + UUID.nameUUIDFromBytes(JB.getBytes()).toString() + "')";
    }

    @AfterAll
    public static void tearDown() throws ServiceFailureException {
        LOGGER.info("Tearing down.");
        cleanup();
    }

    @Override
    protected void setUpVersion() {
        LOGGER.info("Setting up for version {}.", version.urlPart);
        try {
            sMdl = new SensorThingsSensingV11();
            pMdl = new SensorThingsPlus();
            serviceSTAplus = new SensorThingsService(new URL(serverSettings.getServiceUrl(version)), sMdl, pMdl);
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create URL", ex);
        }
    }

    @Override
    protected void tearDownVersion() {
        try {
            cleanup();
        } catch (ServiceFailureException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * CREATE Tests
     */
    @Test
    public void test00UserCreateParty() throws IOException {
        LOGGER.info("  test00UserCreateParty");
        String request = PARTY;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(true, USER_SHOULD_BE_ABLE_TO_CREATE);
            } else {
                fail(response, USER_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    @Test
    public void test100AnonCreateParty() throws IOException {
        LOGGER.info("  test100AnonCreateParty");
        try {
            String request = PARTY;
            HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
            HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

                if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                    Assertions.assertTrue(true, USER_SHOULD_BE_ABLE_TO_CREATE);
                } else {
                    fail(response, USER_SHOULD_BE_ABLE_TO_CREATE);
                }
            }
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create URL", ex);
        }
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
     */
    @Test
    public void test01SameUserCreateParty() throws IOException {
        LOGGER.info("  test01SameUserCreateParty");
        String request = PARTY_LJS;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String location = response.getFirstHeader("Location").getValue();
                Boolean success = (location != null) && location.contains(LJS);
                Assertions.assertTrue(success, SAME_USER_SHOULD_BE_ABLE_TO_CREATE);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
     */
    @Test
    public void test01SameUserCreatePartyNonUUIDUsername() throws IOException {
        LOGGER.info("  test01SameUserCreatePartyNonUUIDUsername");
        String request = PARTY_JB;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, JB, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String location = response.getFirstHeader("Location").getValue();
                Boolean success = (location != null) && location.contains(UUID.nameUUIDFromBytes(JB.getBytes()).toString());
                Assertions.assertTrue(success, SAME_USER_SHOULD_BE_ABLE_TO_CREATE);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 403 Fail: 201
     */
    @Test
    public void test02OtherUserCreateParty() throws IOException {
        LOGGER.info("  test02OtherUserCreateParty");
        String request = PARTY_LJS;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertFalse(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 403 Fail: 201
     */
    @Test
    public void test02OtherNonUUIDUsernameUserCreateParty() throws IOException {
        LOGGER.info("  test02OtherNonUUIDUsernameUserCreateParty");
        String request = PARTY_LJS;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, JB, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertFalse(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
     */
    @Test
    public void test03AdminCreateParty() throws IOException {
        LOGGER.info("  test03AdminCreateParty");
        String request = PARTY_ALICE;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                // we need to make sure that Alice was created
                Assertions.assertTrue(response.getFirstHeader("Location").getValue().equalsIgnoreCase(partyALICEUrl), ADMIN_SHOULD_BE_ABLE_TO_CREATE);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
     */
    @Test
    public void test03AdminCreatePartyNonUUIDUsername() throws IOException {
        LOGGER.info("  test03AdminCreatePartyNonUUIDUsername");
        String request = PARTY_JB;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                // we need to make sure that Alice was created
                Assertions.assertTrue(response.getFirstHeader("Location").getValue().equalsIgnoreCase(partyJBUrl), ADMIN_SHOULD_BE_ABLE_TO_CREATE);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 401 Fail: 201
     */
    @Test
    public void test02AnonCreateParty() throws IOException {
        LOGGER.info("  test02AnonCreateParty");
        String request = PARTY_LJS;
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertFalse(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * UPDATE Tests
     */

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
     */
    @Test
    public void test10SameUserUpdateParty() throws IOException {
        LOGGER.info("  test10SameUserUpdateParty");
        String request = "{\"role\": \"institutional\"}";
        HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE);
            }
        }
    }

    /*
     * SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID Success: 403 Fail: n/a
     */
    @Test
    public void test11SameUserUpdatePartyAuthId() throws IOException {
        LOGGER.info("  test11SameUserUpdatePartyAuthId");
        String request = "{\"authId\": \"505851c3-2de9-4844-9bd5-d185fe944265\"}";
        HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID);
            } else {
                fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 403 Fail: n/a
     */
    @Test
    public void test12OtherUserUpdateParty() throws IOException {
        LOGGER.info("  test12OtherUserUpdateParty");
        String request = "{\"role\": \"institutional\"}";
        HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
     */
    @Test
    public void test13AdminUpdateParty() throws IOException {
        LOGGER.info("  test13AdminUpdateParty");
        String request = "{\"role\": \"institutional\"}";
        HttpPatch httpPatch = new HttpPatch(partyALICEUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 401 Fail: n/a
     */
    @Test
    public void test14AnonUpdateParty() throws IOException {
        LOGGER.info("  test14AnonUpdateParty");
        String request = "{\"role\": \"institutional\"}";
        HttpPatch httpPatch = new HttpPatch(partyALICEUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE);
            }
        }
    }

    /*
     * DELETE Tests
     */

    /*
     * SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
     */
    @Test
    public void test20SameUserDeleteParty() throws IOException {
        LOGGER.info("  test20SameUserDeleteParty");
        HttpDelete httpDelete = new HttpDelete(partyLJSUrl);
        setAuth(httpDelete, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpDelete)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
            } else {
                fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
     */
    @Test
    public void test21OtherUserDeleteParty() throws IOException {
        LOGGER.info("  test21OtherUserDeleteParty");
        HttpDelete httpDelete = new HttpDelete(partyALICEUrl);
        setAuth(httpDelete, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpDelete)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_DELETE Success: 200 Fail: n/a
     */
    @Test
    public void test22AdminDeleteParty() throws IOException {
        LOGGER.info("  test22AdminDeleteParty");
        // Party Alice created by Admin in a previous test
        HttpDelete httpDelete = new HttpDelete(partyALICEUrl);
        setAuth(httpDelete, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                LOGGER.info(ADMIN_SHOULD_BE_ABLE_TO_DELETE);
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_DELETE);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 401 Fail: n/a
     */
    @Test
    public void test23AnonDeleteParty() throws IOException {
        LOGGER.info("  test23AnonDeleteParty");
        HttpDelete httpDelete = new HttpDelete(partyALICEUrl);

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpDelete)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE);
            }
        }
    }

    private void fail(CloseableHttpResponse response, String assertion) throws ParseException, IOException {
        HttpEntity entity = response.getEntity();
        String msg = "";
        if (entity != null) {
            msg = org.apache.http.util.EntityUtils.toString(entity);
        }

        Assertions.fail(assertion, new Throwable(msg));
    }

    public static class Imp10Tests extends PartyTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends PartyTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

}
