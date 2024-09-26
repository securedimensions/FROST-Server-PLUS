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

import static de.securedimensions.frostserver.plugin.staplus.helper.TableHelperLicense.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostclient.SensorThingsService;
import de.fraunhofer.iosb.ilt.frostclient.exception.ServiceFailureException;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsPlus;
import de.fraunhofer.iosb.ilt.frostclient.models.SensorThingsV11Sensing;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.securedimensions.frostserver.plugin.staplus.PluginPLUS;
import de.securedimensions.frostserver.plugin.staplus.test.auth.PrincipalAuthProvider;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andreas Matheus
 */
public abstract class LicenseTests extends AbstractStaPlusTestClass {

    public static class Imp10Tests extends LicenseTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends LicenseTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseTests.class);

    private static final long serialVersionUID = 1639739965;

    private static String PARTY_ALICE = String.format("{\"displayName\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"displayName\": \"ALICE\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);

    private static final String USER_IS_NOT_ABLE_TO_CREATE_MANDATORY_LICENSE = "A user is not able to create any mandatory Licenses re-using the same identifier (@iot.id).";
    private static final String USER_IS_ABLE_TO_CREATE_OWN_LICENSE_CC_BY = "A user is able to create it's own CC-BY License.";

    private static final String USER_IS_NOT_ABLE_TO_CREATE_OWN_LICENSE_CC_PD = "A user is not able to create it's own CC-PD License re-using the same definition.";
    private static String CC_PD_WITH_ID = "{\n" +
            "        \"id\": \"CC_PD\",\n" +
            "        \"name\": \"CC-PD\",\n" +
            "        \"definition\": \"https://creativecommons.org/publicdomain/zero/1.0/\",\n" +
            "        \"description\": \"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication\",\n" +
            "        \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/cc-zero.png\"\n" +
            "    }";
    private static String CC_PD = "{\n" +
            "        \"name\": \"CC-PD\",\n" +
            "        \"definition\": \"https://creativecommons.org/publicdomain/zero/1.0/\",\n" +
            "        \"description\": \"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication\",\n" +
            "        \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/cc-zero.png\"\n" +
            "    }";
    private static String CC_PD_ALICE = "{\n" +
            "        \"name\": \"CC-PD ALice\",\n" +
            "        \"definition\": \"https://creativecommons.org/publicdomain/zero/1.0/\",\n" +
            "        \"description\": \"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication\",\n" +
            "        \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/cc-zero.png\"\n" +
            "    }";

    private static String CC_BY_ALICE = "{\n" +
            "  \"name\": \"CC-BY 3.0 ALice\",\n" +
            "  \"description\": \"The Creative Commons Attribution license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY license\"\n" +
            "}";

    private static String CC_BY_NC_ALICE = "{\n" +
            "  \"name\": \"CC BY-NC 3.0 Alice\",\n" +
            "  \"description\": \"The Creative Commons Attribution-NonCommercial license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by-nc/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY-NC license\"\n" +
            "}\n";

    private static String CC_BY_SA_ALICE = "{\n" +
            "  \"name\": \"CC BY-SA 3.0 Alice\",\n" +
            "  \"description\": \"The Creative Commons Attribution & Share-alike license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by-sa/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-sa.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY-SA license\"\n" +
            "}";

    private static String CC_BY_NC_SA_ALICE = "{\n" +
            "  \"name\": \"CC BY-NC-SA 3.0 Alice\",\n" +
            "  \"description\": \"The Creative Commons Attribution & Share-alike non-commercial license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc-sa.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY-NC-SA license\"\n" +
            "}\n";

    private static String CC_BY_ND_ALICE = "{\n" +
            "  \"name\": \"CC BY-ND 3.0 Alice\",\n" +
            "  \"description\": \"The Creative Commons Attribution & NoDerivs license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by-nd/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nd.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY-ND license\"\n" +
            "}\n";

    private static String CC_BY_NC_ND_ALICE = "{\n" +
            " \"name\": \"CC BY-NC-ND 3.0 Alice\",\n" +
            "  \"description\": \"The Creative Commons Attribution & NonCommercial & NoDerivs license\",\n" +
            "  \"definition\": \"https://creativecommons.org/licenses/by-nc-nd/3.0/deed.en\",\n" +
            "  \"logo\": \"https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-nc-nd.png\",\n" +
            "  \"attributionText\": \"Alice`s cool CC-BY-NC-ND license\"\n" +
            "}\n";

    private static String GROUP(String license) {
        return String.format("{\n"
                + "	\"name\": \"ObservationGroup\",\n"
                + "	\"description\": \"with license\",\n"
                + "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
                + "    \"Party\": " + PARTY_ALICE + ",\n"
                + "    \"License\": %s\n"
                + "}", license);
    }

    private static String DATASTREAM(String license) {
        return String.format("{\n"
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
                + "    \"License\": %s,\n"
                + "    \"Thing\": {\n"
                + "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
                + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
                + "        \"Party\": " + PARTY_ALICE + ",\n"
                + "        \"properties\": {\n"
                + "            \"CPU\": \"1.4GHz\",\n"
                + "            \"RAM\": \"4GB\"\n"
                + "        }\n"
                + "    }\n"
                + "}", license);
    }

    private static String DATASTREAM(int id, String license) {
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
                + "    \"License\": %s,\n"
                + "    \"Thing\": {\n"
                + "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
                + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
                + "        \"Party\": " + PARTY_ALICE + ",\n"
                + "        \"properties\": {\n"
                + "            \"CPU\": \"1.4GHz\",\n"
                + "            \"RAM\": \"4GB\"\n"
                + "        }\n"
                + "    }\n"
                + "}", id, license);
    }

    protected static final String OBSERVATION_DATASTREAM(String license) {
        return String.format("{\n"
                + "    \"phenomenonTime\": \"2021-04-20T02:00:00Z\",\n"
                + "    \"resultTime\": \"2021-04-21T15:43:00Z\",\n"
                + "    \"result\": \"\",\n"
                + "    \"parameters\": {\n"
                + "        \"tilt_angle\": \"30\",\n"
                + "        \"distance\": \"5\",\n"
                + "        \"shutter\": \"2.4\",\n"
                + "        \"speed\": \"1/400\"\n"
                + "    },\n"
                + "    \"Datastream\": " + DATASTREAM(license) + ",\n"
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
                + "}", DATASTREAM(license));
    }

    protected static final String OBSERVATION_DATASTREAM(int id, String license) {
        return String.format("{\n"
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
                + "}", DATASTREAM(id, license));
    }

    protected static final String OBSERVATION_GROUP(String datastream, String license) {
        return String.format("{\n"
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
                + "    \"ObservationGroups\": [%s],\n"
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
                + "}", datastream, GROUP(license));
    }

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
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceLicensing", "true");
        SERVER_PROPERTIES.put("plugins.staplus.enable.enforceObservationGroupLicensing", "false");
        SERVER_PROPERTIES.put("plugins.staplus.idType.license", "String");
        SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
        SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
        // For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per EntityType
        SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
        SERVER_PROPERTIES.put("plugins.coreModel.idType", "LONG");
        SERVER_PROPERTIES.put("plugins.multiDatastream.enable", "true");

    }

    public LicenseTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
    }

    @Override
    protected void setUpVersion() {
        LOGGER.info("Setting up for version {}.", version.urlPart);
        try {
            sMdl = new SensorThingsV11Sensing();
            pMdl = new SensorThingsPlus();
            serviceSTAplus = new SensorThingsService(sMdl, pMdl).setBaseUrl(new URL(serverSettings.getServiceUrl(version))).init();

            for (String k : LICENSES.keySet()) {
                if (!existLicense(k)) {
                    createEntity("/Licenses", LICENSES.get(k));
                }
            }

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

    private int createEntity(String path, String request) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + path);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ADMIN, "");

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

    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
            // Datastream.License, ObservationGroup.License, status code
            {CC_PD_ID, CC_PD_ID, 201},
            {CC_PD_ID, CC_BY_ID, 201},
            {CC_PD_ID, CC_BY_SA_ID, 201},
            {CC_PD_ID, CC_BY_NC_ID, 201},
            {CC_PD_ID, CC_BY_ND_ID, 201},
            {CC_PD_ID, CC_BY_NC_SA_ID, 201},
            {CC_PD_ID, CC_BY_NC_ND_ID, 201},
            {CC_BY_ID, CC_PD_ID, 400},
            {CC_BY_ID, CC_BY_ID, 201},
            {CC_BY_ID, CC_BY_SA_ID, 201},
            {CC_BY_ID, CC_BY_NC_ID, 201},
            {CC_BY_ID, CC_BY_ND_ID, 400},
            {CC_BY_ID, CC_BY_NC_SA_ID, 201},
            {CC_BY_ID, CC_BY_NC_ND_ID, 400},
            {CC_BY_SA_ID, CC_PD_ID, 400},
            {CC_BY_SA_ID, CC_BY_ID, 201},
            {CC_BY_SA_ID, CC_BY_SA_ID, 201},
            {CC_BY_SA_ID, CC_BY_NC_ID, 400},
            {CC_BY_SA_ID, CC_BY_ND_ID, 400},
            {CC_BY_SA_ID, CC_BY_NC_SA_ID, 400},
            {CC_BY_SA_ID, CC_BY_NC_ND_ID, 400},
            {CC_BY_NC_ID, CC_PD_ID, 400},
            {CC_BY_NC_ID, CC_BY_ID, 201},
            {CC_BY_NC_ID, CC_BY_SA_ID, 400},
            {CC_BY_NC_ID, CC_BY_NC_ID, 201},
            {CC_BY_NC_ID, CC_BY_ND_ID, 400},
            {CC_BY_NC_ID, CC_BY_NC_SA_ID, 201},
            {CC_BY_NC_ID, CC_BY_NC_ND_ID, 400},
            {CC_BY_ND_ID, CC_PD_ID, 400},
            {CC_BY_ND_ID, CC_BY_ID, 400},
            {CC_BY_ND_ID, CC_BY_SA_ID, 400},
            {CC_BY_ND_ID, CC_BY_NC_ID, 400},
            {CC_BY_ND_ID, CC_BY_ND_ID, 400},
            {CC_BY_ND_ID, CC_BY_NC_SA_ID, 400},
            {CC_BY_ND_ID, CC_BY_NC_ND_ID, 400},
            {CC_BY_NC_SA_ID, CC_PD_ID, 400},
            {CC_BY_NC_SA_ID, CC_BY_ID, 201},
            {CC_BY_NC_SA_ID, CC_BY_SA_ID, 400},
            {CC_BY_NC_SA_ID, CC_BY_NC_ID, 201},
            {CC_BY_NC_SA_ID, CC_BY_ND_ID, 400},
            {CC_BY_NC_SA_ID, CC_BY_NC_SA_ID, 201},
            {CC_BY_NC_SA_ID, CC_BY_NC_ND_ID, 400},
            {CC_BY_NC_ND_ID, CC_PD_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_SA_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_NC_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_ND_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_NC_SA_ID, 400},
            {CC_BY_NC_ND_ID, CC_BY_NC_ND_ID, 400},});
    }

    @Test
    public void testIdId() throws IOException {
        LOGGER.info("  testIdId");
        for (Object[] d : data()) {
            String datastreamLicenseId = (String) d[0];
            String groupLicenseId = (String) d[1];
            int expectedStatus = (int) d[2];

            int actualStatus = testIdId(datastreamLicenseId, groupLicenseId);
            String test = "TestIdId: Datastream.License(@iot.id=" + datastreamLicenseId + ") - ObservationGroup.License(@iot.id=" + groupLicenseId + ")";
            LOGGER.info(test);
            Assertions.assertTrue(expectedStatus == actualStatus, test);
            if (expectedStatus != actualStatus) {
                LOGGER.info("FAIL: " + test + " expected status code: {} - actual status code: {}", expectedStatus, actualStatus);
            }
        }
    }

    @Test
    public void testCreateNormativeLicense() throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Licenses");
        HttpEntity stringEntity = new StringEntity(CC_PD_WITH_ID, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        // Not allowed to re-use mandatory ids - here CC_PD
        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(true);
            } else {
                fail(response, USER_IS_NOT_ABLE_TO_CREATE_MANDATORY_LICENSE);
            }
        }
    }

    @Test
    public void testCreateOwnLicenseCC_PD() throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Licenses");
        HttpEntity stringEntity = new StringEntity(CC_PD, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        // Not allowed to re-create a license with definition
        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true);
            } else {
                fail(response, USER_IS_NOT_ABLE_TO_CREATE_OWN_LICENSE_CC_PD);
            }
        }
    }

    @Test
    public void testCreateOwnLicenseCC_PD_ALICE() throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Licenses");
        HttpEntity stringEntity = new StringEntity(CC_PD_ALICE, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        // Not allowed to re-create a license with definition
        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(true);
            } else {
                fail(response, USER_IS_NOT_ABLE_TO_CREATE_OWN_LICENSE_CC_PD);
            }
        }
    }

    @Test
    public void testCreateOwnLicenses() throws IOException {

        String requests[] = {CC_BY_ALICE, CC_BY_NC_ALICE, CC_BY_SA_ALICE, CC_BY_NC_SA_ALICE, CC_BY_ND_ALICE, CC_BY_NC_ND_ALICE};

        for (String request : requests) {
            HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Licenses");
            HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            setAuth(httpPost, ALICE, "");
            // Allowed to re-create a CC-BY type license with own attributionText
            try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                    Assertions.assertTrue(true);
                } else {
                    fail(response, USER_IS_ABLE_TO_CREATE_OWN_LICENSE_CC_BY);
                }
            }
        }
    }

    private int testIdId(String datastreamLicenseId, String groupLicenseId) throws IOException {
        String datastreamLicense = "{\"@iot.id\": \"" + datastreamLicenseId + "\"}";
        String groupLicense = "{\"@iot.id\": \"" + groupLicenseId + "\"}";
        String request = OBSERVATION_GROUP(DATASTREAM(datastreamLicense), groupLicense);
        return createObservation(request);

    }

    private int createObservation(String request) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Observations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        setAuth(httpPost, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 201) {
                LOGGER.error(org.apache.http.util.EntityUtils.toString(response.getEntity()));
            }

            return response.getStatusLine().getStatusCode();
        }
    }

    private boolean existLicense(String id) throws IOException {
        String filter = "$filter=@iot.id%20eq%20%27" + id + "%27&$select=@iot.id";
        HttpGet httpGet = new HttpGet(serverSettings.getServiceUrl(version) + "/Licenses?" + filter);
        setAuth(httpGet, ALICE, "");

        try (CloseableHttpResponse response = serviceSTAplus.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(org.apache.http.util.EntityUtils.toString(response.getEntity()));
                return false;
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());

                return (jsonNode.get("value").get(0).get("@iot.id").textValue().equalsIgnoreCase(id));
            }
        }
    }

}
