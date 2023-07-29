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
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests for the encodingType of Location and FeatureOfInterest entity types.
 * The Core conformance class requires encodingType=application/geo+json for creating a Location or FeatureOfInterest.
 * For Update, the encodingType may be not exist, but if set, the value must be application/geo+json
 * A Location can be deleted, but a FeatureOfInterest cannot.
 *
 * @author Andreas Matheus
 */
public abstract class StorageCRSTests extends AbstractStaPlusTestClass {

    public static class Imp10Tests extends StorageCRSTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends StorageCRSTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageCRSTests.class);

    private static final long serialVersionUID = 1639739965;

    private static String PARTY_ALICE = String.format("{\"displayName\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"displayName\": \"ALICE\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);

    private static String THING_EXISTING_PARTY = "{\n"
            + "    \"id\": \"1\",\n"
            + "    \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
            + "    \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
            + "    \"properties\": {\n"
            + "        \"CPU\": \"1.4GHz\",\n"
            + "        \"RAM\": \"4GB\"\n"
            + "    },\n"
            + "    \"Party\": {\n"
            + "        \"@iot.id\": \"%s\"\n"
            + "    }\n"
            + "}";

    private static String DATASTREAM(int id) {
        return String.format("{\n"
                + "    \"@iot.id\": %d,\n"
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
                + "    \"Party\": " + PARTY_ALICE + ",\n"
                + "    \"License\": {\n"
                + "            \"name\": \"CC BY 3.0\",\n"
                + "            \"definition\": \"https://creativecommons.org/licenses/by/3.0/deed.en\",\n"
                + "            \"description\": \"The Creative Commons Attribution license\",\n"
                + "            \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by.png\"\n"
                + "        },\n"
                + "    \"Thing\": {\n"
                + "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
                + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
                + "        \"Party\": " + PARTY_ALICE + ",\n"
                + "        \"properties\": {\n"
                + "            \"CPU\": \"1.4GHz\",\n"
                + "            \"RAM\": \"4GB\"\n"
                + "        }\n"
                + "    }\n"
                + "}", id);
    }

    protected static final String OBSERVATION(int observationId, int datastreamId, int foiId) {
        return String.format("{\n"
                + "    \"@iot.id\": %d,\n"
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
                + "        \"@iot.id\": %d,\n"
                + "        \"name\": \"The observed boundary\",\n"
                + "        \"description\": \"The actual real world area observed\",\n"
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
                + "}", observationId, DATASTREAM(datastreamId), foiId);
    }

    private static final String LOCATION = "{\n"
            + "    \"id\": \"%d\",\n"
            + "    \"name\": \"My Garden\",\n"
            + "    \"description\": \"The north facing part of the property\",\n"
            + "    \"encodingType\": \"%s\",\n"
            + "    \"location\": {\n"
            + "        \"type\": \"Point\",\n"
            + "        \"coordinates\": [\n"
            + "            0,\n"
            + "            1\n"
            + "        ]\n"
            + "    },\n"
            + "    \"properties\": {\n"
            + "        \"city\": \"Munich\",\n"
            + "        \"countryCode\": \"DE\"\n"
            + "    }\n"
            + "}";

    private static final String LOCATION_NO_ENCODING_TYPE = "{\n"
            + "    \"id\": \"%d\",\n"
            + "    \"name\": \"My Garden\",\n"
            + "    \"description\": \"The north facing part of the property\",\n"
            + "    \"location\": {\n"
            + "        \"type\": \"Point\",\n"
            + "        \"coordinates\": [\n"
            + "            0,\n"
            + "            1\n"
            + "        ]\n"
            + "    },\n"
            + "    \"properties\": {\n"
            + "        \"city\": \"Munich\",\n"
            + "        \"countryCode\": \"DE\"\n"
            + "    }\n"
            + "}";


    private static final String FOI = "{\n"
            + "    \"id\": \"%d\",\n"
            + "    \"name\": \"My Garden\",\n"
            + "    \"description\": \"The north facing part of the property\",\n"
            + "    \"encodingType\": \"%s\",\n"
            + "    \"feature\": {\n"
            + "        \"type\": \"Point\",\n"
            + "        \"coordinates\": [\n"
            + "            0,\n"
            + "            1\n"
            + "        ]\n"
            + "    },\n"
            + "    \"properties\": {\n"
            + "        \"city\": \"Munich\",\n"
            + "        \"countryCode\": \"DE\"\n"
            + "    }\n"
            + "}";

    private static final String FOI_NO_ENCODING_TYPE = "{\n"
            + "    \"id\": \"%d\",\n"
            + "    \"name\": \"My Garden\",\n"
            + "    \"description\": \"The north facing part of the property\",\n"
            + "    \"feature\": {\n"
            + "        \"type\": \"Point\",\n"
            + "        \"coordinates\": [\n"
            + "            0,\n"
            + "            1\n"
            + "        ]\n"
            + "    },\n"
            + "    \"properties\": {\n"
            + "        \"city\": \"Munich\",\n"
            + "        \"countryCode\": \"DE\"\n"
            + "    }\n"
            + "}";

    protected static final String LOCATION(int locationId, String encodingType, int thingId) {
        return String.format(LOCATION, locationId, encodingType, thingId);
    }

    protected static final String LOCATION_NO_ENCODING_TYPE(int locationId, int thingId) {
        return String.format(LOCATION_NO_ENCODING_TYPE, locationId, thingId);
    }

    protected static final String FOI(int foiId, String encodingType) {
        return String.format(FOI, foiId, encodingType);
    }

    protected static final String FOI_NO_ENCODING_TYPE(int foiId) {
        return String.format(FOI_NO_ENCODING_TYPE, foiId);
    }

    private static final String CREATE_LOCATION_WITH_GEOJSON = "Create Location with encodingType=application/geo+json.";
    private static final String CREATE_LOCATION_WRONG_ENCODING_TYPE = "Create Location with wrong encodingType=wkt.";
    private static final String CREATE_LOCATION_NO_ENCODING_TYPE = "Create Location with no encodingType.";


    private static final String UPDATE_LOCATION_WITH_GEOJSON = "Update Location with encodingType=application/geo+json.";
    private static final String UPDATE_LOCATION_WRONG_ENCODING_TYPE = "Update Location with encodingType=wkt.";
    private static final String UPDATE_LOCATION_NO_ENCODING_TYPE = "Update Location with no encodingType.";

    private static final String CREATE_FOI_WITH_GEOJSON = "Create FeatureOfInterest with encodingType=application/geo+json.";
    private static final String CREATE_FOI_WRONG_ENCODING_TYPE = "Create FeatureOfInterest with wrong encodingType=wkt.";
    private static final String CREATE_FOI_NO_ENCODING_TYPE = "Create FeatureOfInterest with no encodingType.";

    private static final String UPDATE_FOI_WITH_GEOJSON = "Update FeatureOfInterest with encodingType=application/geo+json.";
    private static final String UPDATE_FOI_WRONG_ENCODING_TYPE = "Update FeatureOfInterest with encodingType=wkt.";
    private static final String UPDATE_FOI_NO_ENCODING_TYPE = "Update FeatureOfInterest with no encodingType.";


    private static final int HTTP_CODE_200 = 200;
    private static final int HTTP_CODE_201 = 201;
    private static final int HTTP_CODE_400 = 400;

    private static final Map<String, String> SERVER_PROPERTIES = new LinkedHashMap<>();

    static {
        SERVER_PROPERTIES.put("plugins.plugins", PluginPLUS.class.getName());
        SERVER_PROPERTIES.put("plugins.staplus.enable", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceOwnership", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceLicensing", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceGroupLicensing", "false");
        SERVER_PROPERTIES.put("plugins.staplus.idType.license", "String");
        SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
        SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
        SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
        SERVER_PROPERTIES.put("plugins.coreModel.idType", "LONG");
        //SERVER_PROPERTIES.put("plugins.plus.idType.group", "String");
        //SERVER_PROPERTIES.put("plugins.coreModel.idType.datastream", "String");
        //SERVER_PROPERTIES.put("plugins.multiDatastream.idType.multiDatastream", "String");
        SERVER_PROPERTIES.put("plugins.multiDatastream.enable", "true");

    }

    public StorageCRSTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
    }

    @Override
    protected void setUpVersion() {
        LOGGER.info("Setting up for version {}.", version.urlPart);
        try {
            sMdl = new SensorThingsSensingV11();
            pMdl = new SensorThingsPlus(sMdl);
            serviceSTAplus = new SensorThingsService(pMdl.getModelRegistry(), new URL(serverSettings.getServiceUrl(version)));

            createEntity("/Parties", PARTY_ALICE, ALICE);
            createEntity("/Things", THING_EXISTING_PARTY.formatted(ALICE), ALICE);

        } catch (IOException ex) {
            LOGGER.error("Failed to execute request", ex);
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

    private int createEntity(String path, String request, String authId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + path);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, authId, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 201) {
                LOGGER.error(org.apache.http.util.EntityUtils.toString(response.getEntity()));
            }

            return response.getStatusLine().getStatusCode();
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

    /*
    Location Tests
     */

    @Test
    public void testCreateLocation() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateLocation");
        String request = LOCATION(1, "application/geo+json", 1);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things(1)/Locations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(true, CREATE_LOCATION_WITH_GEOJSON);
            } else {
                fail(response, CREATE_LOCATION_WITH_GEOJSON);
            }
        }
    }

    @Test
    public void testCreateLocationWrongEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateLocationWrongEncodingType");
        String request = LOCATION(1, "wkt", 1);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things(1)/Locations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, CREATE_LOCATION_WRONG_ENCODING_TYPE);
            } else {
                fail(response, CREATE_LOCATION_WRONG_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testCreateLocationNoEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateLocationNoEncodingType");
        String request = LOCATION_NO_ENCODING_TYPE(1, 1);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things(1)/Locations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, CREATE_LOCATION_NO_ENCODING_TYPE);
            } else {
                fail(response, CREATE_LOCATION_NO_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testUpdateLocation() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateLocation");
        String request = LOCATION(100, "application/geo+json", 1);
        createEntity("/Things(1)/Locations", request, ALICE);

        request = LOCATION(1, "application/geo+json", 1);
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/Locations(100)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(true, UPDATE_LOCATION_WITH_GEOJSON);
            } else {
                fail(response, UPDATE_LOCATION_WITH_GEOJSON);
            }
        }
    }

    @Test
    public void testUpdateLocationWrongEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateLocationWrongEncodingType");
        String request = LOCATION(101, "application/geo+json", 1);
        createEntity("/Things(1)/Locations", request, ALICE);

        request = LOCATION(101, "wkt", 1);
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/Locations(101)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, UPDATE_LOCATION_WRONG_ENCODING_TYPE);
            } else {
                fail(response, UPDATE_LOCATION_WRONG_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testUpdateLocationNoEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateLocationNoEncodingType");
        String request = LOCATION(102, "application/geo+json", 1);
        createEntity("/Things(1)/Locations", request, ALICE);

        request = LOCATION_NO_ENCODING_TYPE(102, 1);
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/Locations(102)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                Assertions.assertTrue(true, UPDATE_LOCATION_NO_ENCODING_TYPE);
            } else {
                fail(response, UPDATE_LOCATION_NO_ENCODING_TYPE);
            }
        }
    }

    /*
    FeatureOfInterest Tests
     */
    @Test
    public void testCreateFoI() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateFeatureOfInterest");
        String request = FOI(1, "application/geo+json");
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(true, CREATE_FOI_WITH_GEOJSON);
            } else {
                fail(response, CREATE_FOI_WITH_GEOJSON);
            }
        }
    }

    @Test
    public void testCreateFoIWrongEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateFeatureOfInterestWrongEncodingType");
        String request = FOI(2, "wkt");
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, CREATE_FOI_WRONG_ENCODING_TYPE);
            } else {
                fail(response, CREATE_FOI_WRONG_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testCreateFoINoEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testCreateFeatureOfInterestNoEncodingType");
        String request = FOI_NO_ENCODING_TYPE(3);
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, CREATE_FOI_NO_ENCODING_TYPE);
            } else {
                fail(response, CREATE_FOI_NO_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testUpdateFoI() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateFoI");

        createEntity("/Datastreams", DATASTREAM(100), ALICE);
        createEntity("/Observations", OBSERVATION(100, 100, 100), ALICE);

        String request = FOI(100, "application/geo+json");
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest(100)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, UPDATE_FOI_WITH_GEOJSON);
            } else {
                fail(response, UPDATE_FOI_WITH_GEOJSON);
            }
        }
    }

    @Test
    public void testUpdateFoIWrongEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateFoIWrongEncodingType");

        createEntity("/Datastreams", DATASTREAM(101), ALICE);
        createEntity("/Observations", OBSERVATION(101, 101, 101), ALICE);

        String request = FOI(101, "wkt");
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest(101)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, UPDATE_FOI_WRONG_ENCODING_TYPE);
            } else {
                fail(response, UPDATE_FOI_WRONG_ENCODING_TYPE);
            }
        }
    }

    @Test
    public void testUpdateFoINoEncodingType() throws ClientProtocolException, IOException {
        LOGGER.info("  testUpdateFoINoEncodingType");

        createEntity("/Datastreams", DATASTREAM(102), ALICE);
        createEntity("/Observations", OBSERVATION(102, 102, 102), ALICE);

        String request = FOI_NO_ENCODING_TYPE(102);
        HttpPatch httpPatch = new HttpPatch(serverSettings.getServiceUrl(version) + "/FeaturesOfInterest(102)");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        setAuth(httpPatch, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPatch)) {

            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true, UPDATE_FOI_NO_ENCODING_TYPE);
            } else {
                fail(response, UPDATE_FOI_NO_ENCODING_TYPE);
            }
        }
    }

}
