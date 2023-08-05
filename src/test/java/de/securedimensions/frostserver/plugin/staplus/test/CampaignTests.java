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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the Campaign class properties. According to the ownership concept, a
 * Campaign's properties can only be changed by the user that 'owns' the Campaign
 * instance. That user has the same UUID as the Party's authId property.
 *
 * @author Andreas Matheus
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class CampaignTests extends AbstractStaPlusTestClass {

    public static class Imp10Tests extends CampaignTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends CampaignTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignTests.class);

    private static final long serialVersionUID = 1639739965;

    private static final String CAMPAIGN_MUST_HAVE_A_PARTY = "Campaign must have a Party.";

    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE = "Admin should be able to create.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE = "Admin should be able to update.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE_PARTY = "Admin should be able to update Party.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE = "Admin should be able to delete.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY = "Same user should be able to create Campaign associated with Party in request.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY = "Same user should be able to create Campaign associated with existing Party.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE = "Same user should be able to update.";
    private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_OTHER_PARTY = "Same user should not be able to update with other existing Party.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE = "Same User should NOT be able to delete.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE = "Other user should NOT be able to create Campaign.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE = "Other user should NOT be able to update.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY = "Other user should NOT be able to update Party.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Other user should NOT be able to delete.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE = "anon should NOT be able to create.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE = "anon should NOT be able to update.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE = "anon should NOT be able to delete.";
    private static final String ANY_USER_SHOULD_BE_ABLE_TO_ADD_OBSERVATION = "Any user should be able to add Observation.";

    private static String CAMPAIGN = "{\n"
            + "	\"name\": \"Campaign\",\n"
            + "	\"description\": \"none\",\n"
            + "	\"creationTime\": \"2021-12-12T12:12:12Z\",\n"
            + "	\"termsOfUse\": \"none\"\n"
            + "}";

    private static String CAMPAIGN_INLINE_PARTY = "{\n"
            + "	 \"name\": \"Campaign with Party inline\",\n"
            + "  \"description\": \"none\",\n"
            + "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
            + "	 \"termsOfUse\": \"none\",\n"
            + "	 \"License\": {\"@iot.id\": \"CC_BY\"},\n"
            + "  \"Party\": {\n"
            + "        \"displayName\": \"Long John Silver Citizen Scientist\",\n"
            + "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
            + "        \"role\": \"individual\",\n"
            + "        \"authId\": \"%s\"\n"
            + "  }\n"
            + "}";

    private static String CAMPAIGN_EXISTING_PARTY = "{\n"
            + "	 \"name\": \"Campaign with Party external\",\n"
            + "	 \"description\": \"none\",\n"
            + "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
            + "	 \"termsOfUse\": \"none\",\n"
            + "	 \"License\": {\"@iot.id\": \"CC_BY\"},\n"
            + "  \"Party\": {\n"
            + "        \"authId\": \"%s\","
            + "          \"role\":  \"individual\"\n"
            + "  }\n"
            + "}";

    private static String DATASTREAM_PARTY = "{\n"
            + "    \"unitOfMeasurement\": {\n"
            + "        \"name\": \"n/a\",\n"
            + "        \"symbol\": \"\",\n"
            + "        \"definition\": \"https://www.merriam-webster.com/dictionary/picture\"\n"
            + "    },\n"
            + "    \"name\": \"photo datastream\",\n"
            + "    \"description\": \"this datastream is about pictures\",\n"
            + "    \"observationType\": \"http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement\",\n"
            + "    \"ObservedProperty\": {\n"
            + "        \"name\": \"Picture\",\n"
            + "        \"definition\": \"https://www.merriam-webster.com/dictionary/picture\",\n"
            + "        \"description\": \"The image taken by the camera (the sensor)\"\n"
            + "    },\n"
            + "    \"Sensor\": {\n"
            + "        \"name\": \"Pi NoIR - Raspberry Pi Infrared Camera Module\",\n"
            + "        \"description\": \"Sony IMX 219 PQ CMOS image sensor in a fixed-focus module with IR blocking filter removed\",\n"
            + "        \"encodingType\": \"application/pdf\",\n"
            + "        \"metadata\": \"https://cdn-reichelt.de/documents/datenblatt/A300/RASP_CAN_2.pdf\"\n"
            + "    },\n"
            + "    \"License\": {\n"
            + "        \"name\": \"CC0\",\n"
            + "        \"definition\": \"https://creativecommons.org/publicdomain/zero/1.0/\",\n"
            + "        \"description\": \"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication\",\n"
            + "        \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/cc-zero.png\"\n"
            + "    },\n"
            + "    \"Party\": {\n"
            + "        \"displayName\": \"Long John Silver Citizen Scientist\",\n"
            + "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
            + "        \"role\": \"individual\",\n"
            + "        \"authId\": \"%s\"\n"
            + "    },\n"
            + "    \"Thing\": {\n"
            + "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
            + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
            + "        \"properties\": {\n"
            + "            \"CPU\": \"1.4GHz\",\n"
            + "            \"RAM\": \"4GB\"\n"
            + "        },\n"
            + "         \"Party\": {\n"
            + "             \"displayName\": \"Long John Silver Citizen Scientist\",\n"
            + "             \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
            + "             \"role\": \"individual\",\n"
            + "             \"authId\": \"%s\"\n"
            + "         }\n"
            + "    }\n"
            + "}";

    private static final String OBSERVATION = "{\n"
            + "    \"phenomenonTime\": \"2021-04-20T02:00:00Z\",\n"
            + "    \"resultTime\": \"2021-04-21T15:43:00Z\",\n"
            + "    \"result\": \"\",\n"
            + "    \"parameters\": {\n"
            + "        \"tilt_angle\": \"30\",\n"
            + "        \"distance\": \"5\",\n"
            + "        \"shutter\": \"2.4\",\n"
            + "        \"speed\": \"1/400\"\n"
            + "    },\n"
            + "    \"Datastream\": %s,\n"
            + "    \"FeatureOfInterest\": {\n"
            + "        \"name\": \"The observed boundary\",\n"
            + "        \"description\": \"The actual real worl area observed\",\n"
            + "        \"encodingType\": \"application/geo+json\",\n"
            + "        \"feature\": {\n"
            + "            \"type\": \"Feature\",\n"
            + "            \"geometry\": {\n"
            + "              \"type\": \"Polygon\",\n"
            + "              \"coordinates\": [\n"
            + "                [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],\n"
            + "                  [100.0, 1.0], [100.0, 0.0] ]\n"
            + "                ]\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";

    private static String OBSERVATION_LJS = String.format(OBSERVATION, String.format(DATASTREAM_PARTY, LJS, LJS));

    private static String PARTY_EXISTING = "{\"@iot.id\": \"%s\"}";
    private static String PARTY = "{\"displayName\": \"Party\", \"description\": \"I'm a test Party\", \"role\": \"individual\", \"authId\": \"%s\"}";
    private static String PARTY_ALICE = String.format("{\"displayName\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
    private static String PARTY_LJS = String.format("{\"displayName\": \"Long John Silver Citizen Scientist\", \"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);

    private static final int HTTP_CODE_200 = 200;
    private static final int HTTP_CODE_201 = 201;
    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_401 = 401;
    private static final int HTTP_CODE_403 = 403;

    private static final Map<String, String> SERVER_PROPERTIES = new LinkedHashMap<>();

    static {
        SERVER_PROPERTIES.put("plugins.plugins", PluginPLUS.class.getName());
        SERVER_PROPERTIES.put("plugins.staplus.enable", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceOwnership", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceLicensing", "false");
        SERVER_PROPERTIES.put("plugins.staplus.idType.license", "String");
        SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
        SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
        SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
        SERVER_PROPERTIES.put("plugins.coreModel.idType", "LONG");
        SERVER_PROPERTIES.put("plugins.multiDatastream.enable", "true");
    }

    //private static SensorThingsService service;
    private String partyLJS, partyALICE;

    public CampaignTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
        // This is the party that we are going to apply the Update and Delete tests
        // on...
        partyLJS = serverSettings.getServiceUrl(version) + "/Parties('" + LJS + "')";
        partyALICE = serverSettings.getServiceUrl(version) + "/Parties('" + ALICE + "')";
    }

    @Override
    protected void setUpVersion() {
        LOGGER.info("Setting up for version {}.", version.urlPart);
        try {
            sMdl = new SensorThingsSensingV11();
            pMdl = new SensorThingsPlus(sMdl);
            serviceSTAplus = new SensorThingsService(pMdl.getModelRegistry(), new URL(serverSettings.getServiceUrl(version)));
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

    @AfterAll
    public static void tearDown() throws ServiceFailureException {
        LOGGER.info("Tearing down.");
        cleanup();
    }

    /*
     * CAMPAIGN_MUST_HAVE_A_PARTY Success: 400 Fail: n/a
     */
    @Test
    public void test00CampaignMustHaveAParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test00CampaignMustHaveAParty");
        String request = CAMPAIGN;

        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, CAMPAIGN_MUST_HAVE_A_PARTY);
            } else {
                fail(response, CAMPAIGN_MUST_HAVE_A_PARTY);
            }
        }
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test01SameUserCreateCampaignInlineParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test01SameUserCreateCampaignInlineParty");
        String request = String.format(CAMPAIGN_INLINE_PARTY, LJS);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String location = response.getFirstHeader("Location").getValue();
                HttpGet httpGet = new HttpGet(location + "/Party");
                try (CloseableHttpResponse response2 = serviceSTAplus.execute(httpGet)) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> map = mapper.readValue(response2.getEntity().getContent(), Map.class);

                    Assertions.assertTrue(map.get("authId").equalsIgnoreCase(LJS), SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY);
                }
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY);
            }
        }
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test01SameUserCreateCampaignExistingParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test01SameUserCreateCampaignExistingParty");
        createParty(LJS);

        String request = String.format(CAMPAIGN_EXISTING_PARTY, LJS);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String location = response.getFirstHeader("Location").getValue();
                HttpGet httpGet = new HttpGet(location + "/Party");
                try (CloseableHttpResponse response2 = serviceSTAplus.execute(httpGet)) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> map = mapper.readValue(response2.getEntity().getContent(), Map.class);

                    Assertions.assertTrue(map.get("authId").equalsIgnoreCase(LJS), SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY);
                }
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 403 Fail: 201
     */
    @Test
    public void test02OtherUserCreateCampaignAssoc() throws ClientProtocolException, IOException {
        LOGGER.info("  test02OtherUserCreateCampaignAssoc");
        String request = String.format(CAMPAIGN_INLINE_PARTY, LJS);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
     */
    @Test
    public void test03AdminCreateCampaignAssoc() throws ClientProtocolException, IOException {
        LOGGER.info("  test03AdminCreateCampaignAssoc");
        String request = String.format(CAMPAIGN_INLINE_PARTY, ALICE);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                // we need to make sure that the Campaign is associated with Alice
                String location = response.getFirstHeader("Location").getValue();
                HttpGet httpGet = new HttpGet(location + "/Party");
                try (CloseableHttpResponse response2 = serviceSTAplus.execute(httpGet)) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> map = mapper.readValue(response2.getEntity().getContent(), Map.class);

                    Assertions.assertTrue(map.get("authId").equalsIgnoreCase(ALICE), ADMIN_SHOULD_BE_ABLE_TO_CREATE);
                }
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 401 Fail: 201
     */
    @Test
    public void test02AnonCreateCampaignAssoc() throws ClientProtocolException, IOException {
        LOGGER.info("  test02AnonCreateCampaignAssoc");
        String request = String.format(CAMPAIGN_INLINE_PARTY, LJS);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
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
    private String createCampaignParty(String userId) throws IOException {
        String request = String.format(CAMPAIGN_INLINE_PARTY, userId);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Campaigns");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, userId, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            return response.getFirstHeader("Location").getValue();
        }
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
     */
    @Test
    public void test12SameUserUpdateCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test12SameUserUpdateCampaign");
        String campaignUrl = createCampaignParty(LJS);

        String request = "{\"name\": \"foo bar\"}";
        HttpPatch httpPatch = new HttpPatch(campaignUrl);
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
     * SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test10SameUserUpdateCampaignOtherParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test10SameUserUpdateCampaignOtherParty");
        createParty(ALICE);
        String groupUrl = createCampaignParty(LJS);

        String request = "{\"Party\":" + String.format(PARTY_EXISTING, ALICE) + "}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_OTHER_PARTY);
            } else {
                fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_OTHER_PARTY);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 403 Fail: n/a
     */
    @Test
    public void test12OtherUserUpdateCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test12OtherUserUpdateCampaign");
        String groupUrl = createCampaignParty(LJS);

        String request = "{\"name\": \"foo bar\"}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
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
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test10OtherUserUpdateCampaignParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test10OtherUserUpdateCampaignParty");
        createParty(ALICE);

        String groupUrl = createCampaignParty(LJS);

        String request = "{\"Party\":" + String.format(PARTY_EXISTING, ALICE) + "}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
     */
    @Test
    public void test13AdminUpdateCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test13AdminUpdateCampaign");
        String groupUrl = createCampaignParty(LJS);

        String request = "{\"name\": \"foo bar\"}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
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
     * ADMIN_SHOULD_BE_ABLE_TO_UPDATE_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test10AdminUpdateCampaignParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test10AdminUpdateCampaignParty");
        createParty(ALICE);

        String groupUrl = createCampaignParty(LJS);

        String request = "{\"Party\":" + String.format(PARTY_EXISTING, ALICE) + "}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ADMIN, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_PARTY);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_PARTY);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 401 Fail: n/a
     */
    @Test
    public void test14AnonUpdateCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test14AnonUpdateCampaign");
        String groupUrl = createCampaignParty(LJS);

        String request = "{\"name\": \"foo bar\"}";
        HttpPatch httpPatch = new HttpPatch(groupUrl);
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
     * SAME_USER_SHOULD_BE_ABLE_TO_DELETE Success: 200 Fail: n/a
     */
    @Test
    public void test20SameUserDeleteCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test20SameUserDeleteCampaign");
        String groupUrl = createCampaignParty(LJS);
        HttpDelete httpDelete = new HttpDelete(groupUrl);
        setAuth(httpDelete, LJS, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_DELETE);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
     */
    @Test
    public void test21OtherUserDeleteCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test21OtherUserDeleteCampaign");
        String groupUrl = createCampaignParty(LJS);
        HttpDelete httpDelete = new HttpDelete(groupUrl);
        setAuth(httpDelete, ALICE, "");

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
    public void test22AdminDeleteCampaign() throws ClientProtocolException, IOException {
        LOGGER.info("  test22AdminDeleteCampaign");
        String groupUrl = createCampaignParty(ALICE);
        HttpDelete httpDelete = new HttpDelete(groupUrl);
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
    public void test23AnonDeleteParty() throws ClientProtocolException, IOException {
        LOGGER.info("  test23AnonDeleteParty");
        HttpDelete httpDelete = new HttpDelete(partyALICE);

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
            msg = new String(entity.getContent().readAllBytes());
        }

        Assertions.fail(assertion, new Throwable(msg));
    }

    private String createParty(String userId) throws IOException {
        String request = String.format(PARTY, userId);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, userId, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            return response.getFirstHeader("Location").getValue();
        }
    }

}
