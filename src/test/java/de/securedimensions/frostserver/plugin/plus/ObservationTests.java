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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.statests.AbstractTestClass;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.securedimensions.frostserver.plugin.plus.auth.PrincipalAuthProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the Observation class properties. According to the ownership
 * concept, a Observation's properties can only be changed by the user that
 * 'owns' the Observation via the Datastream instance.
 *
 * @author Andreas Matheus
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class ObservationTests extends AbstractTestClass {

    public static class Imp10Tests extends ObservationTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends ObservationTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationTests.class);

    private static final long serialVersionUID = 1639739965;

    private static final String DATASTREAM_MUST_HAVE_A_PARTY = "Datastream must have a Party.";

    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY = "Same user should be able to create Observation with Datastream and Party inline.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY = "Same user should be able to create Observation with Datastream and existing Party.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM = "Same user should be able to create Observation with existing Datastream.";

    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY = "Same user should be able to update Observation with Datastream and Party inline.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY = "Same user should be able to update Observation with Datastream and existing Party.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM = "Same user should be able to update Observation with existing Datastream.";

    private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY = "Same user should be able to update Observation with Datastream and Party inline.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY = "Same user should be able to update Observation with Datastream and existing Party.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM = "Same user should be able to update Observation with existing Datastream.";

    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY = "Other user should NOT be able to create Observation with Datastream and Party inline.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY = "Other user should NOT be able to create Observation with Datastream and existing Party.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM = "Other user should be able to create Observation with existing Datastream.";

    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY = "Other user should NOT be able to update Observation with Datastream and Party inline.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY = "Other user should NOT be able to update Observation with Datastream and existing Party.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM = "Other user should NOT be able to update Observation with existing Datastream.";

    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY = "Other user should NOT be able to delete Observation with Datastream and Party inline.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY = "Other user should NOT be able to delete Observation with Datastream and existing Party.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM = "Other user should NOT be able to update Observation with existing Datastream.";

    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY = "Admin should be able to create Observation with Datastream and Party inline.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY = "Admin should be able to create Observation with Datastream and existing Party.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM = "Admin should be able to create Observation with existing Datastream.";

    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY = "Admin should be able to update Observation with Datastream and Party inline.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY = "Admin should be able to update Observation with Datastream and existing Party.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM = "Admin should be able to update Observation with existing Datastream.";

    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY = "Admin should be able to delete Observation with Datastream and Party inline.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY = "Admin should be able to delete Observation with Datastream and existing Party.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM = "Admin should be able to delete Observation with existing Datastream.";

    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY = "anon should NOT be able to create Observation with Datastream and Party inline.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY = "anon should NOT be able to create Observation with Datastream and existing Party.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM = "anon should NOT be able to create Observation with existing Datastream.";

    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY = "anon should NOT be able to update Observation with Datastream and Party inline.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY = "anon should NOT be able to update Observation with Datastream and existing Party.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM = "anon should NOT be able to update Observation with existing Datastream.";

    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY = "anon should NOT be able to delete Observation with Datastream and Party inline.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY = "anon should NOT be able to delete Observation with Datastream and existing Party.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM = "anon should NOT be able to delete Observation with existing Datastream.";

    public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
    public static final String LJS = "21232f29-7a57-35a7-8389-4a0e4a801fc3";
    public static final String ADMIN = "admin";

    private static String PARTY_ALICE = String.format("{\"name\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
    private static String PARTY_LJS = String.format("{\"name\": \"Long John Silver Citizen Scientist\", \"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);

    private static String DATASTREAM_INLINE_PARTY = "{\n"
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
            + "        \"name\": \"Long John Silver Citizen Scientist\",\n"
            + "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
            + "        \"role\": \"individual\",\n"
            + "        \"authId\": \"%s\"\n"
            + "    },\n"
            + "    \"Thing\": {\n"
            + "        \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
            + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
            + "        \"properties\": {\n"
            + "            \"CPU\": \"1.4GHz\",\n"
            + "            \"RAM\": \"4GB\"\n"
            + "        },\n"
            + "        \"Party\": {\"@iot.id\": \"%s\"}\n"
            + "    }\n"
            + "}";

    private static String DATASTREAM_EXISTING_PARTY = "{\n"
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
            + "        \"@iot.id\": \"%s\"\n"
            + "    },\n"
            + "    \"Thing\": {\n"
            + "        \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
            + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
            + "        \"properties\": {\n"
            + "            \"CPU\": \"1.4GHz\",\n"
            + "            \"RAM\": \"4GB\"\n"
            + "        },\n"
            + "        \"Party\": {\"@iot.id\": \"%s\"}\n"
            + "    }\n"
            + "}";

    private static String DATASTREAM_EXISTING = "{\"@iot.id\": %s}";

    private static String DATASTREAM_INLINE_PARTY(String userId) {
        return String.format(DATASTREAM_INLINE_PARTY, userId, userId);
    }

    private static String DATASTREAM_EXISTING_PARTY(String userId) {
        return String.format(DATASTREAM_EXISTING_PARTY, userId, userId);
    }

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

    private static String OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(String userId) {
        return String.format(OBSERVATION, DATASTREAM_INLINE_PARTY(userId));
    }

    private static String OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(String userId) {
        return String.format(OBSERVATION, DATASTREAM_EXISTING_PARTY(userId));
    }

    private static String OBSERVATION_EXISTING_DATASTREAM(String dsId) {
        return String.format(String.format(OBSERVATION, String.format(DATASTREAM_EXISTING, dsId)));
    }

    private static final int HTTP_CODE_200 = 200;
    private static final int HTTP_CODE_201 = 201;
    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_401 = 401;
    private static final int HTTP_CODE_403 = 403;

    private static final Properties SERVER_PROPERTIES = new Properties();

    static {
        SERVER_PROPERTIES.put("plugins.plugins", PluginPLUS.class.getName());
        SERVER_PROPERTIES.put("plugins.plus.enable", true);
        SERVER_PROPERTIES.put("plugins.plus.enable.enforceOwnsership", true);
        SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
        SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
        SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
        SERVER_PROPERTIES.put("plugins.coreModel.idType", "LONG");
        SERVER_PROPERTIES.put("plugins.plus.idType.license", "String");
        SERVER_PROPERTIES.put("plugins.multiDatastream.enable", true);
    }

    private static SensorThingsService service;

    public ObservationTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
    }

    @Override
    protected void setUpVersion() {
        LOGGER.info("Setting up for version {}.", version.urlPart);
        try {
            service = new SensorThingsService(new URL(serverSettings.getServiceUrl(version)));
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create URL", ex);
        }
    }

    @Override
    protected void tearDownVersion() throws ServiceFailureException {
        cleanup();
    }

    @AfterAll
    public static void tearDown() throws ServiceFailureException {
        LOGGER.info("Tearing down.");
        cleanup();
    }

    private static void cleanup() throws ServiceFailureException {

    }

    private CloseableHttpResponse createObservation(String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Observations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId == null) {
            unsetAuth(service);
        } else {
            setAuth(service, userId, "");
        }

        return service.execute(httpPost);
    }

    private String createParty(String userId) throws IOException {
        String request = (userId.equalsIgnoreCase(ALICE)) ? PARTY_ALICE : PARTY_LJS;

        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId == null) {
            unsetAuth(service);
        } else {
            setAuth(service, userId, "");
        }

        CloseableHttpResponse response = service.execute(httpPost);

        return response.getFirstHeader("Location").getValue();
    }

    private String createDatastream(String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId == null) {
            unsetAuth(service);
        } else {
            setAuth(service, userId, "");
        }

        CloseableHttpResponse response = service.execute(httpPost);

        return response.getFirstHeader("Location").getValue();
    }

    private String getEntityId(String entityUrl) throws ParseException, IOException {
        HttpGet httpGet = new HttpGet(entityUrl);
        CloseableHttpResponse response = service.execute(httpGet);

        String json = org.apache.http.util.EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(json, Map.class);

        return String.valueOf(map.get("@iot.id"));

    }

    /*
==== BASE ====
	 * CREATE Tests
     */
 /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test10SameUserCreateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test11SameUserCreateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM Success: 201 Fail: n/a
     */
    @Test
    public void test12SameUserCreateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test20OtherUserCreateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), ALICE);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test21OtherUserCreateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), ALICE);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM Success: 403 Fail: n/a
     */
    @Test
    public void test22OtherUserCreateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), ALICE);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test30AnonCreateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), null);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test31AnonCreateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), null);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM Success: 401 Fail: n/a
     */
    @Test
    public void test32AnonCreateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), null);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test40AdminCreateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), ADMIN);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 201 Fail: n/a
     */
    @Test
    public void test41AdminCreateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), ADMIN);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM Success: 201 Fail: n/a
     */
    @Test
    public void test42AdminCreateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), ADMIN);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * DELETE Tests
     */
    private CloseableHttpResponse deleteObservation(String observationUrl, String userId) throws IOException {
        HttpDelete httpDelete = new HttpDelete(observationUrl);
        if (userId == null) {
            unsetAuth(service);
        } else {
            setAuth(service, userId, "");
        }

        return service.execute(httpDelete);
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test50SameUserCreateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), LJS);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test51SameUserDeleteObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), LJS);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM Success: 201 Fail: n/a
     */
    @Test
    public void test52SameUserDeleteObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);
        response = deleteObservation(response.getFirstHeader("Location").getValue(), LJS);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test60OtherUserDeleteObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test61OtherUserDeleteObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM Success: 403 Fail: n/a
     */
    @Test
    public void test62OtherUserDeleteObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test70AnonDeleteObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test71AnonDeleteObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM Success: 401 Fail: n/a
     */
    @Test
    public void test72AnonDeleteObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test80AdminDeleteObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test81AdminDeleteObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM Success: 200 Fail: n/a
     */
    @Test
    public void test82AdminCreateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), ADMIN);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * UPDATE Tests
     */
    private CloseableHttpResponse updateObservation(String observationUrl, String userId) throws IOException {
        String request = "{\"result\": \"foo bar\"}";

        HttpPatch httpPatch = new HttpPatch(observationUrl);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        if (userId == null) {
            unsetAuth(service);
        } else {
            setAuth(service, userId, "");
        }

        return service.execute(httpPatch);
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test50SameUserUpdateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), LJS);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test51SameUserUpdateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), LJS);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM Success: 201 Fail: n/a
     */
    @Test
    public void test52SameUserUpdateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);
        response = updateObservation(response.getFirstHeader("Location").getValue(), LJS);

        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        } else {
            fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test60OtherUserUpdateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 403 Fail: n/a
     */
    @Test
    public void test61OtherUserUpdateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM Success: 403 Fail: n/a
     */
    @Test
    public void test62OtherUserUpdateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ALICE);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
            Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        } else {
            fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test70AnonUpdateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 401 Fail: n/a
     */
    @Test
    public void test71AnonUpdateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = deleteObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM Success: 401 Fail: n/a
     */
    @Test
    public void test72AnonUpdateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), null);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
            Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        } else {
            fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test80AdminUpdateObservation() throws ClientProtocolException, IOException {
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_INLINE_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_INLINE_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY Success: 200 Fail: n/a
     */
    @Test
    public void test81AdminUpdateObservation() throws ClientProtocolException, IOException {
        String partyUrl = createParty(LJS);
        CloseableHttpResponse response = createObservation(OBSERVATION_INLINE_DATASTREAM_EXTERNAL_PARTY(LJS), LJS);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_INLINE_DATASTREAM_EXISTING_PARTY);
        }
    }

    /*
	 * ADMIN_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM Success: 200 Fail: n/a
     */
    @Test
    public void test82AdminUpdateObservation() throws ClientProtocolException, IOException {
        String datastreamUrl = createDatastream(DATASTREAM_INLINE_PARTY(LJS), LJS);
        String datastreamId = getEntityId(datastreamUrl);

        CloseableHttpResponse response = createObservation(OBSERVATION_EXISTING_DATASTREAM(datastreamId), ADMIN);

        response = updateObservation(response.getFirstHeader("Location").getValue(), ADMIN);
        if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
            Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        } else {
            fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_EXISTING_DATASTREAM);
        }
    }

    private static void unsetAuth(SensorThingsService service) {
        service.setHttpClient(HttpClientBuilder.create().build());
    }

    private static void setAuth(SensorThingsService service, String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        URL url = service.getEndpoint();

        credsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(username, password));

        service.setHttpClient(HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build());
    }

    private void fail(CloseableHttpResponse response, String assertion) throws ParseException, IOException {
        HttpEntity entity = response.getEntity();
        String msg = "";
        if (entity != null) {
            msg = org.apache.http.util.EntityUtils.toString(entity);
        }

        LOGGER.error(assertion, msg);
        Assertions.fail(assertion);
    }

}
