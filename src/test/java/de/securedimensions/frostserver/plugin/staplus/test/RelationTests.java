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
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the Relation class properties. According to the data model a
 * Relation must have a Subject. A Relation must have either Object or
 * externalObject
 *
 * @author Andreas Matheus
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class RelationTests extends AbstractStaPlusTestClass {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationTests.class);
    private static final long serialVersionUID = 1639739965;
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION = "Same user should be able to create Relation.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP = "Same user should be able to create Relation to own Group.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION = "Same user should be able to update Relation.";
    private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_RELATION = "Same user should be able to delete Relation.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION = "Other user should NOT be able to create Relation to other party's observation.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP = "User should NOT be able to create Relation to other user's Group.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION = "Other user should NOT be able to update Relation to other party's observation.";
    private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION = "Other user should NOT be able to delete Relation to other party's observation.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION = "Admin should be able to create Relation to other party's observation.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP = "Admin should be able to create Relation to other party's observation and group.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION = "Admin should be able to update Relation to other party's observation.";
    private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_RELATION = "Admin should be able to delete Relation to other party's observation.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION = "anon should NOT be able to create Relation.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP = "anon should NOT be able to create Relation with Group.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION = "anon should NOT be able to update Relation.";
    private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION = "anon should NOT be able to delete Relation.";
    private static final String OBSERVATION = "{\n"
            + "    \"id\": %d,\n"
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
            + "}";
    private static final String DATASTREAM_INLINE_PARTY = "{\n"
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
            + "        \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
            + "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
            + "        \"properties\": {\n"
            + "            \"CPU\": \"1.4GHz\",\n"
            + "            \"RAM\": \"4GB\"\n"
            + "        },\n"
            + "        \"Party\": {\"@iot.id\": \"%s\"}\n"
            + "    }\n"
            + "}";
    private static final String RELATION_NO_SUBJECT = "{\n"
            + "\"role\": \"error\", \n"
            + "\"description\": \"Relation must have a Subject\", \n"
            + "}";
    private static final String RELATION_NO_OBJECT = "{\n"
            + "\"role\": \"error\", \n"
            + "\"description\": \"Relation must have an Object\", \n"
            + "\"Subject\": {\"@iot.id\": 1},\n"
            + "}";
    private static final String RELATION_SUBJECT_OBJECT = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": 1},\n"
            + "\"Object\": {\"@iot.id\": 2}\n"
            + "}";

    private static final String RELATION_MDS_SUBJECT_OBJECT = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with MultiDatastream Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": 100},\n"
            + "\"Object\": {\"@iot.id\": 2}\n"
            + "}";
    private static final String RELATION_SUBJECT_OBJECT_GROUP = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": 1},\n"
            + "\"Object\": {\"@iot.id\": 2},\n"
            + "\"Groups\": [{\"@iot.id\": %d}]\n"
            + "}";
    private static final String RELATION_OBJECT_EXTOBJECT = "{\n"
            + "\"role\": \"error\", \n"
            + "\"description\": \"Relation must have either Object or externalObject\", \n"
            + "\"Subject\": {\"@iot.id\": 1},\n"
            + "\"Object\": {\"@iot.id\": 2}\n"
            + "\"externalObject\": {\"@iot.id\": \"http://localhost/404\"}\n"
            + "}";
    private static final String RELATION_EXTERNAL_OBSERVATIONS = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": %d},\n"
            + "\"Object\": {\"@iot.id\": %d}\n"
            + "}";
    private static final String RELATION_EXTERNAL_OBSERVATIONS_GROUP = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": %d},\n"
            + "\"Object\": {\"@iot.id\": %d},\n"
            + "\"Groups\": [{\"@iot.id\": %d}]\n"
            + "}";
    private static final String RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP = "{\n"
            + "\"role\": \"OK\", \n"
            + "\"description\": \"proper Relation with Subject and Object\", \n"
            + "\"Subject\": {\"@iot.id\": %d},\n"
            + "\"Object\": {\"@iot.id\": %d},\n"
            + "\"Groups\": [%s]\n"
            + "}";
    private static final int HTTP_CODE_200 = 200;
    private static final int HTTP_CODE_201 = 201;
    private static final int HTTP_CODE_400 = 400;
    private static final int HTTP_CODE_401 = 401;
    private static final int HTTP_CODE_403 = 403;
    private static final Map<String, String> SERVER_PROPERTIES = new LinkedHashMap<>();
    private static final String GROUP_INLINE_LJS = "{\n"
            + "  \"id\": %d,\n"
            + "	 \"name\": \"Group with LJS inline\",\n"
            + "  \"description\": \"none\",\n"
            + "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
            + "    \"Party\": {\n"
            + "        \"displayName\": \"Long John Silver Citizen Scientist\",\n"
            + "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
            + "        \"role\": \"individual\",\n"
            + "        \"authId\": \"21232f29-7a57-35a7-8389-4a0e4a801fc3\"\n"
            + "    }\n"
            + "}";
    private static final String GROUP_INLINE_ALICE = "{\n"
            + "  \"id\": %d,\n"
            + "	 \"name\": \"Group with ALICE inline\",\n"
            + "  \"description\": \"none\",\n"
            + "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
            + "    \"Party\": {\n"
            + "        \"displayName\": \"Alice in Wonderland\",\n"
            + "        \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\",\n"
            + "        \"role\": \"individual\",\n"
            + "        \"authId\": \"505851c3-2de9-4844-9bd5-d185fe944265\"\n"
            + "    }\n"
            + "}";

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

    public RelationTests(ServerVersion version) {
        super(version, SERVER_PROPERTIES);
    }

    private static String DATASTREAM_INLINE_PARTY(String userId) {
        return String.format(DatastreamTests.DATASTREAM_PARTY, userId, userId);
    }

    private static String OBSERVATION_INLINE_PARTY(String userId, int observationId) {
        String ds = DATASTREAM_INLINE_PARTY(userId);
        return OBSERVATION(observationId, ds);
    }

    private static String OBSERVATION(int observationId, String ds) {
        return String.format(OBSERVATION, observationId, ds);
    }

    private static String GROUP_INLINE_LJS(int groupId) {
        return String.format(GROUP_INLINE_LJS, groupId);
    }

    private static String GROUP_INLINE_ALICE(int groupId) {
        return String.format(GROUP_INLINE_ALICE, groupId);
    }

    private static String RELATION_EXTERNAL_OBSERVATIONS(int o1, int o2) {
        return String.format(RELATION_EXTERNAL_OBSERVATIONS, o1, o2);
    }

    private static String RELATION_EXTERNAL_OBSERVATIONS_GROUP(int o1, int o2, int g) {
        return String.format(RELATION_EXTERNAL_OBSERVATIONS_GROUP, o1, o2, g);
    }

    private static String RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP(int o1, int o2, String g) {
        return String.format(RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP, o1, o2, g);
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
            pMdl = new SensorThingsPlus(sMdl);
            serviceSTAplus = new SensorThingsService(pMdl.getModelRegistry(), new URL(serverSettings.getServiceUrl(version)));

            try (CloseableHttpResponse r1 = createObservation(OBSERVATION_INLINE_PARTY(LJS, 1), LJS)) {
                if (r1.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                    LOGGER.error("Failed to create Observation no. 1");
                }
            }
            try (CloseableHttpResponse r2 = createObservation(OBSERVATION_INLINE_PARTY(LJS, 2), LJS)) {
                if (r2.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                    LOGGER.error("Failed to create Observation no. 2");
                }
            }
            try (CloseableHttpResponse r3 = createGroup(GROUP_INLINE_LJS(1), LJS)) {
                if (r3.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                    LOGGER.error("Failed to create Group no. 1");
                }
            }
            try (CloseableHttpResponse r4 = createGroup(GROUP_INLINE_ALICE(2), ALICE)) {
                if (r4.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                    LOGGER.error("Failed to create Group no. 2");
                }
            }
            /*
             * Create MultiDatastream
             */
            String mdsUrl = serverSettings.getServiceUrl(version) + "/MultiDatastreams";
            try (CloseableHttpResponse r = create(mdsUrl, MultiDatastreamTests.MULTIDATASTREAM_PARTY.formatted(LJS, LJS), LJS)) {
                if (r.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                    LOGGER.error("Failed to create MultiDatastream");
                }
                String obsUrl = r.getFirstHeader("Location").getValue() + "/Observations";

                try (CloseableHttpResponse r1 = create(obsUrl, MultiDatastreamTests.MDS_OBSERVATION_ID.formatted(100), LJS)) {
                    if (r1.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                        LOGGER.error("Failed to create MultiDatastream Observation no. 1");
                    }
                }
                try (CloseableHttpResponse r2 = create(obsUrl, MultiDatastreamTests.MDS_OBSERVATION_ID.formatted(101), LJS)) {
                    if (r2.getStatusLine().getStatusCode() != HTTP_CODE_201) {
                        LOGGER.error("Failed to create MultiDatastream Observation no. 1");
                    }
                }
            }
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to create URL", ex);
        } catch (IOException e) {
            LOGGER.error("Failed to create Entity", e);
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

    private CloseableHttpResponse create(String url, String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId != null) {
            setAuth(httpPost, userId, "");
        }

        return serviceSTAplus.execute(httpPost);

    }

    private CloseableHttpResponse createObservation(String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Observations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId != null) {
            setAuth(httpPost, userId, "");
        }

        return serviceSTAplus.execute(httpPost);
    }

    private CloseableHttpResponse createRelation(String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Relations");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId != null) {
            setAuth(httpPost, userId, "");
        }

        return serviceSTAplus.execute(httpPost);
    }

    private CloseableHttpResponse createGroup(String request, String userId) throws IOException {
        HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Groups");
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        if (userId != null) {
            setAuth(httpPost, userId, "");
        }

        return serviceSTAplus.execute(httpPost);
    }

    /*
     * ==== BASE ====
     * CREATE Tests
     */
    @Test
    public void test00CreateRelation() throws IOException {
        LOGGER.info("  test00CreateRelation");
        final String CREATE_RELATION = "Create Relation with Subject and Object.";
        try (CloseableHttpResponse response = createRelation(RELATION_SUBJECT_OBJECT, LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION);
            } else {
                fail(response, CREATE_RELATION);
            }
        }
    }

    @Test
    public void test00CreateRelationMDS() throws IOException {
        LOGGER.info("  test00CreateRelationMDS");
        final String CREATE_RELATION = "Create Relation with MultiDatastream Subject and Datastream Object.";
        try (CloseableHttpResponse response = createRelation(RELATION_MDS_SUBJECT_OBJECT, LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION);
            } else {
                fail(response, CREATE_RELATION);
            }
        }
    }

    @Test
    public void test00CreateRelationGroup() throws IOException {
        LOGGER.info("  test00CreateRelationGroup");
        final String CREATE_RELATION = "Create Relation with Subject, Object and Group.";
        try (CloseableHttpResponse response = createRelation(String.format(RELATION_SUBJECT_OBJECT_GROUP, 1), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION);
            } else {
                fail(response, CREATE_RELATION);
            }
        }
    }

    @Test
    public void test00CreateRelationNoSubject() throws IOException {
        LOGGER.info("  test00CreateRelationNoSubject");
        final String CREATE_RELATION_NO_SUBJECT = "Create Relation with no Subject.";
        try (CloseableHttpResponse response = createRelation(RELATION_NO_SUBJECT, LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION_NO_SUBJECT);
            } else {
                fail(response, CREATE_RELATION_NO_SUBJECT);
            }
        }
    }

    @Test
    public void test00CreateRelationNoObject() throws IOException {
        LOGGER.info("  test00CreateRelationNoObject");
        final String CREATE_RELATION_NO_OBJECT = "Create Relation with no Object.";
        try (CloseableHttpResponse response = createRelation(RELATION_NO_OBJECT, LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION_NO_OBJECT);
            } else {
                fail(response, CREATE_RELATION_NO_OBJECT);
            }
        }
    }

    @Test
    public void test00CreateRelationObjectExtObject() throws IOException {
        LOGGER.info("  test00CreateRelationObjectExtObject");
        final String CREATE_RELATION_OBJECT_EXTOBJECT = "Create Relation with Object and extObject.";
        try (CloseableHttpResponse response = createRelation(RELATION_OBJECT_EXTOBJECT, LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_400) {
                Assertions.assertTrue(Boolean.TRUE, CREATE_RELATION_OBJECT_EXTOBJECT);
            } else {
                fail(response, CREATE_RELATION_OBJECT_EXTOBJECT);
            }
        }
    }

    /*
     * CREATE Tests
     */
    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION Success: 201 Fail: n/a
     */
    @Test
    public void test10SameUserCreateRelation() throws IOException {
        LOGGER.info("  test10SameUserCreateRelation");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10SameUserCreateRelationMDS() throws IOException {
        LOGGER.info("  test10SameUserCreateRelationMDS");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(100, 2), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10SameUserCreateRelationGroup() throws IOException {
        LOGGER.info("  test10SameUserCreateRelationGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_GROUP(1, 2, 1 /* LJS */), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10SameUserCreateRelationInternalGroup() throws IOException {
        LOGGER.info("  test10SameUserCreateRelationInternalGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP(1, 2, GROUP_INLINE_LJS(100)), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION Success: 403 Fail: n/a
     */
    @Test
    public void test20OtherUserCreateRelation() throws IOException {
        LOGGER.info("  test20OtherUserCreateRelation");
        /*
         * The Datastream (and so the observations) is associated to LJS
         */
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), ALICE)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test20OtherUserCreateRelationMDS() throws IOException {
        LOGGER.info("  test20OtherUserCreateRelationMDS");
        /*
         * The Datastream (and so the observations) is associated to LJS
         */
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(100, 101), ALICE)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10OtherUserCreateRelationGroup() throws IOException {
        LOGGER.info("  test10OtherUserCreateRelationGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_GROUP(1, 2, 2 /* ALICE */), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    @Test
    public void test10OtherUserCreateRelationInternalGroup() throws IOException {
        LOGGER.info("  test10OtherUserCreateRelationInternalGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP(1, 2, GROUP_INLINE_ALICE(101)), LJS)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION Success: 401 Fail: n/a
     */
    @Test
    public void test30AnonCreateRelation() throws IOException {
        LOGGER.info("  test30AnonCreateRelation");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), null)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10AnonUserCreateRelationGroup() throws IOException {
        LOGGER.info("  test10AnonUserCreateRelationGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_GROUP(1, 2, 2 /* ALICE */), null)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    @Test
    public void test10AnonUserCreateRelationInternalGroup() throws IOException {
        LOGGER.info("  test10AnonUserCreateRelationInternalGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP(1, 2, GROUP_INLINE_ALICE(102)), null)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION Success: 201 Fail: n/a
     */
    @Test
    public void test40AdminCreateRelation() throws IOException {
        LOGGER.info("  test40AdminCreateRelation");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), ADMIN)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION);
            }
        }
    }

    @Test
    public void test10AdminCreateRelationGroup() throws IOException {
        LOGGER.info("  test10AdminCreateRelationGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_GROUP(1, 2, 2 /* ALICE */), ADMIN)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    @Test
    public void test10AdminCreateRelationInternalGroup() throws IOException {
        LOGGER.info("  test10AdminCreateRelationInternalGroup");
        try (CloseableHttpResponse response = createRelation(RELATION_EXTERNAL_OBSERVATIONS_INTERNAL_GROUP(1, 2, GROUP_INLINE_ALICE(103)), ADMIN)) {
            if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            } else {
                fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE_RELATION_GROUP);
            }
        }
    }

    /*
     * DELETE Tests
     */
    private CloseableHttpResponse deleteRelation(String url, String userId) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url);
        if (userId != null) {
            setAuth(httpDelete, userId, "");
        }

        return serviceSTAplus.execute(httpDelete);
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_RELATION Success: 200 Fail: n/a
     */
    @Test
    public void test50SameUserDeleteRelation() throws IOException {
        LOGGER.info("  test50SameUserDeleteRelation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = deleteRelation(url, LJS)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_RELATION);
                    } else {
                        fail(r2, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION Success: 403 Fail: n/a
     */
    @Test
    public void test60OtherUserDeleteRelation() throws IOException {
        LOGGER.info("  test60OtherUserDeleteRelation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = deleteRelation(url, ALICE)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                        Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION);
                    } else {
                        fail(r2, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION Success: 401 Fail: n/a
     */
    @Test
    public void test70AnonDeleteRelation() throws IOException {
        LOGGER.info("  test70AnonDeleteRelation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = deleteRelation(url, null)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                        Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION);
                    } else {
                        fail(r2, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_DELETE_RELATION Success: 200 Fail: n/a
     */
    @Test
    public void test80AdminDeleteObservation() throws IOException {
        LOGGER.info("  test80AdminDeleteObservation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = deleteRelation(url, ADMIN)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_DELETE_RELATION);
                    } else {
                        fail(r2, ADMIN_SHOULD_BE_ABLE_TO_DELETE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * UPDATE Tests
     */
    private CloseableHttpResponse updateRelationExternalSubject(String url, String userId) throws IOException {
        String request = "{\"description\": \"foo bar\", \"Subject\": {\"@iot.id\": 1}}";

        HttpPatch httpPatch = new HttpPatch(url);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        if (userId != null) {
            setAuth(httpPatch, userId, "");
        }

        return serviceSTAplus.execute(httpPatch);
    }

    private CloseableHttpResponse updateRelationInternalSubject(String url, String subject, String userId) throws IOException {
        String request = "{\"Subject\": %s}".formatted(subject);

        HttpPatch httpPatch = new HttpPatch(url);
        HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
        httpPatch.setEntity(stringEntity);
        if (userId != null) {
            setAuth(httpPatch, userId, "");
        }

        return serviceSTAplus.execute(httpPatch);
    }

    /*
     * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION Success: 200 Fail: n/a
     */
    @Test
    public void test50SameUserUpdateRelationExternalSubject() throws IOException {
        LOGGER.info("  test50SameUserUpdateRelationExternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, LJS)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test50SameUserUpdateRelationInternalSubject() throws IOException {
        LOGGER.info("  test50SameUserUpdateRelationInternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationInternalSubject(url, OBSERVATION_INLINE_PARTY(LJS, 2), LJS)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION Success: 403 Fail: n/a
     */
    @Test
    public void test60OtherUserUpdateRelationExternalSubject() throws IOException {
        LOGGER.info("  test60OtherUserUpdateRelationExternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, ALICE)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                        Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test50OtherUserUpdateRelationInternalSubject() throws IOException {
        LOGGER.info("  test50OtherUserUpdateRelationInternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationInternalSubject(url, OBSERVATION_INLINE_PARTY(ALICE, 2), LJS)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_403) {
                        Assertions.assertTrue(Boolean.TRUE, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION Success: 401 Fail: n/a
     */
    @Test
    public void test70AnonUpdateRelation() throws IOException {
        LOGGER.info("  test70AnonUpdateRelation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, null)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                        Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test60OAnonUserUpdateRelationExternalSubject() throws IOException {
        LOGGER.info("  test60OAnonUserUpdateRelationExternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, null)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                        Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test50AnonUserUpdateRelationInternalSubject() throws IOException {
        LOGGER.info("  test50AnonUserUpdateRelationInternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationInternalSubject(url, OBSERVATION_INLINE_PARTY(ALICE, 2), null)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_401) {
                        Assertions.assertTrue(Boolean.TRUE, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    /*
     * ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION Success: 200 Fail: n/a
     */
    @Test
    public void test80AdminUpdateRelation() throws IOException {
        LOGGER.info("  test80AdminUpdateRelation");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, ADMIN)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test50AdminUpdateRelationExternalSubject() throws IOException {
        LOGGER.info("  test50AdminUpdateRelationExternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationExternalSubject(url, ADMIN)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
            }
        }
    }

    @Test
    public void test50AdminUpdateRelationInternalSubject() throws IOException {
        LOGGER.info("  test50AdminUpdateRelationInternalSubject");
        try (CloseableHttpResponse r1 = createRelation(RELATION_EXTERNAL_OBSERVATIONS(1, 2), LJS)) {
            if (r1.getStatusLine().getStatusCode() == HTTP_CODE_201) {
                String url = r1.getFirstHeader("Location").getValue();
                try (CloseableHttpResponse r2 = updateRelationInternalSubject(url, OBSERVATION_INLINE_PARTY(ALICE, 2), ADMIN)) {
                    if (r2.getStatusLine().getStatusCode() == HTTP_CODE_200) {
                        Assertions.assertTrue(Boolean.TRUE, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    } else {
                        fail(r2, ADMIN_SHOULD_BE_ABLE_TO_UPDATE_RELATION);
                    }
                }
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

    public static class Imp10Tests extends RelationTests {

        public Imp10Tests() {
            super(ServerVersion.v_1_0);
        }
    }

    public static class Imp11Tests extends RelationTests {

        public Imp11Tests() {
            super(ServerVersion.v_1_1);
        }
    }

}
