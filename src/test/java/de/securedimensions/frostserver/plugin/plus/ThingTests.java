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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.statests.AbstractTestClass;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.securedimensions.frostserver.plugin.plus.auth.PrincipalAuthProvider;

/**
 * Tests for the Thing class properties. According to the ownership concept, a
 * Thing's properties can only be changed by the user that 'owns' the Thing
 * instance. That user has the same UUID as the Party's authId property.
 *
 * @author Andreas Matheus
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ThingTests extends AbstractTestClass {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ThingTests.class);

    private static final long serialVersionUID = 1639739965;

	private static final String THING_MUST_HAVE_A_PARTY = "Thing must have a Party.";

	private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE = "Admin should be able to create.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE = "Admin should be able to update.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE = "Admin should be able to delete.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY = "Same user should be able to create Thing associated with own Party from request.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY = "Same user should be able to create Thing associated with own pre-existing Party.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE = "Same user should be able to update.";
	private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY = "Same user should be able to update Party.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE = "Same User should be able to delete.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_PARTY = "Other user should NOT be able to create Thing with Party from request.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_PARTY = "Other user should NOT be able to create Thing with existing Party.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE = "Other user should NOT be able to update.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Other user should NOT be able to delete.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE = "anon should NOT be able to create.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE = "anon should NOT be able to update.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE = "anon should NOT be able to delete.";

	private static final String SAME_USER_SHOULD_BE_ABLE_TO_ADD_LOCATION = "Same user should be able to add Location.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION = "Other user should NOT be able to add Location.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_ADD_LOCATION = "Admin user should be able to add Location.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION = "Anon should NOT be able to add Location.";

	private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_LOCATION = "Same user should be able to delete Location.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION = "Other user should NOT be able to delete Location.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_LOCATION = "Admin user should be able to delete Location.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION = "Anon should NOT be able to delete Location.";

	private static final String SAME_USER_SHOULD_BE_ABLE_TO_ADD_DATASTREAM = "Same user should be able to add Datastream.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM = "Other user should NOT be able to add Datastream.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_ADD_DATASTREAM = "Admin user should be able to add Datastream.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM = "Anon should NOT be able to add Datastream.";

	private static final String SAME_USER_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM = "Same user should be able to delete Datastream.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM = "Other user should NOT be able to delete Datastream.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM = "Admin user should be able to delete Datastream.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM = "Anon should NOT be able to delete Datastream.";

	public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
	public static final String LJS = "21232f29-7a57-35a7-8389-4a0e4a801fc3";
	public static final String ADMIN = "admin";

	private static final String  THING = "{\n"
			+ "    \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
			+ "    \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
			+ "    \"properties\": {\n"
			+ "        \"CPU\": \"1.4GHz\",\n"
			+ "        \"RAM\": \"4GB\"\n"
			+ "    }\n"
			+ "}";
	
	private static String PARTY = "{\"name\": \"Party\", \"description\": \"\", \"role\": \"institutional\", \"authId\": \"%s\"}";
	private static String PARTY_ALICE = String.format("{\"name\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
	private static String PARTY_LJS = String.format("{\"name\": \"Long John Silver Citizen Scientist\", \"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);
	
	private static String THING_INLINE_PARTY = "{\n"
			+ "    \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
			+ "    \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
			+ "    \"properties\": {\n"
			+ "        \"CPU\": \"1.4GHz\",\n"
			+ "        \"RAM\": \"4GB\"\n"
			+ "    },\n"
			+ "    \"Party\": {\n"
			+ "        \"name\": \"Long John Silver Citizen Scientist\",\n"
			+ "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
			+ "        \"role\": \"individual\",\n"
			+ "        \"authId\": \"%s\"\n"
			+ "    }\n"
			+ "}";

	private static String THING_EXISTING_PARTY = "{\n"
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

	private static final String LOCATION = "{\n"
			+ "    \"name\": \"My Garden\",\n"
			+ "    \"description\": \"The north facing part of the property\",\n"
			+ "    \"encodingType\": \"application/geo+json\",\n"
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
	
	private static final String LOCATION_ID = "{\n"
			+ "    \"@iot.id\": %s,\n"
			+ "    \"name\": \"My Garden\",\n"
			+ "    \"description\": \"The north facing part of the property\",\n"
			+ "    \"encodingType\": \"application/geo+json\",\n"
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
			+ "        \"name\": \"Long John Silver Citizen Scientist\",\n"
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
			+ "    \"Party\": {\n"
			+ "        \"name\": \"Long John Silver Citizen Scientist\",\n"
			+ "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
			+ "        \"role\": \"individual\",\n"
			+ "        \"authId\": \"%s\"\n"
			+ "    }\n"
			+ "    }\n"
			+ "}";

	private static String DATASTREAM_PARTY_ID = "{\n"
			+ "    \"unitOfMeasurement\": {\n"
			+ "        \"name\": \"n/a\",\n"
			+ "        \"symbol\": \"\",\n"
			+ "        \"definition\": \"https://www.merriam-webster.com/dictionary/picture\"\n"
			+ "    },\n"
			+ "    \"@iot.id\": %s,\n"
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
			+ "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
			+ "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
			+ "        \"properties\": {\n"
			+ "            \"CPU\": \"1.4GHz\",\n"
			+ "            \"RAM\": \"4GB\"\n"
			+ "        },\n"
			+ "    \"Party\": {\n"
			+ "        \"name\": \"Long John Silver Citizen Scientist\",\n"
			+ "        \"description\": \"The opportunistic pirate by Robert Louis Stevenson\",\n"
			+ "        \"role\": \"individual\",\n"
			+ "        \"authId\": \"%s\"\n"
			+ "    }\n"
			+ "    }\n"
			+ "}";

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
		SERVER_PROPERTIES.put("plugins.coreModel.idType","LONG");
		SERVER_PROPERTIES.put("auth.allowAnonymousRead", true);
		SERVER_PROPERTIES.put("plugins.coreModel.idType","LONG");
		SERVER_PROPERTIES.put("plugins.plus.idType.license", "String");
		SERVER_PROPERTIES.put("plugins.multiDatastream.enable", true);
	}

	//private static SensorThingsService service;

	private static int locationId = 1000;
	private static int datastreamId = 1000;

	public ThingTests(ServerVersion version) throws ServiceFailureException, IOException, URISyntaxException {
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

	@AfterClass
	public static void tearDown() throws ServiceFailureException {
		LOGGER.info("Tearing down.");
		cleanup();
	}

	private static void cleanup() throws ServiceFailureException {
		//EntityUtils.deleteAll(version, serverSettings, service);
	}

	/*
	 * CREATE Tests
	 */

	/*
	 * THING_MUST_HAVE_A_PARTY Success: 400 Fail: n/a
	 */
	@Test
	public void test00ThingMustHaveAParty() throws ClientProtocolException, IOException {
		String request = THING;
		
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_400)
		{
			Assert.assertTrue(THING_MUST_HAVE_A_PARTY, Boolean.TRUE);
		} else {
			fail(response, THING_MUST_HAVE_A_PARTY);
		}
	}

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY Success: 201 Fail: n/a
	 */
	@Test
	public void test01SameUserCreateThingInlineParty() throws ClientProtocolException, IOException {
		String request = String.format(THING_INLINE_PARTY,LJS);
		
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			String location = response.getFirstHeader("Location").getValue();
			HttpGet httpGet = new HttpGet(location + "/Party");
			response = service.execute(httpGet);
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(response.getEntity().getContent(), Map.class);
			
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY,
					map.get("authId").equalsIgnoreCase(LJS));
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_INLINE_PARTY);
		}

	}

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY Success: 201 Fail: n/a
	 */
	@Test
	public void test01SameUserCreateThingExistingParty() throws ClientProtocolException, IOException {
		createParty(LJS);
		
		String request = String.format(THING_EXISTING_PARTY,LJS);
		
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			String location = response.getFirstHeader("Location").getValue();
			HttpGet httpGet = new HttpGet(location + "/Party");
			response = service.execute(httpGet);
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(response.getEntity().getContent(), Map.class);
			
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY,
					map.get("authId").equalsIgnoreCase(LJS));
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_EXISTING_PARTY);
		}

	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_PARTY Success: 403 Fail: 201
	 */
	@Test
	public void test02OtherUserCreateThingInlineParty() throws ClientProtocolException, IOException {
		String request = String.format(THING_INLINE_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_PARTY, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_INLINE_PARTY);
		}

	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_PARTY Success: 403 Fail: 201
	 */
	@Test
	public void test02OtherUserCreateThingExistingParty() throws ClientProtocolException, IOException {
		createParty(LJS);
		
		String request = String.format(THING_EXISTING_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_PARTY, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_EXISTING_PARTY);
		}

	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
	 */
	@Test
	public void test03AdminCreateThingAssoc() throws ClientProtocolException, IOException {
		String request = String.format(THING_INLINE_PARTY,ALICE);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			// we need to make sure that the Thing is associated with Alice
			String location = response.getFirstHeader("Location").getValue();
			HttpGet httpGet = new HttpGet(location + "/Party");
			response = service.execute(httpGet);
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(response.getEntity().getContent(), Map.class);
			
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_CREATE,
					map.get("authId").equalsIgnoreCase(ALICE));
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 401 Fail: 201
	 */
	@Test
	public void test02AnonCreateThingAssoc() throws ClientProtocolException, IOException {
		String request = String.format(THING_INLINE_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);

		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_CREATE, Boolean.TRUE);
		} else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertFalse(ANON_SHOULD_NOT_BE_ABLE_TO_CREATE, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_CREATE);
		}
	}

	/*
	 * UPDATE Tests
	 */

	private String createThingParty(String userId) throws IOException
	{
		String request = String.format(THING_INLINE_PARTY,userId);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Things");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, userId, "");
		CloseableHttpResponse response = service.execute(httpPost);

		return response.getFirstHeader("Location").getValue();
	}
	
	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = "{\"name\": \"foo bar\"}";
		HttpPatch httpPatch = new HttpPatch(thingURL);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPatch);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_UPDATE, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE);
		}
	}

	/*
	 * SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY Success: 403 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateThingParty() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = "{\"Party\":" + PARTY_ALICE + "}";
		HttpPatch httpPatch = new HttpPatch(thingURL);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPatch);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 403 Fail: n/a
	 */
	@Test
	public void test12OtherUserUpdateThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE);
		}
	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
	 */
	@Test
	public void test13AdminUpdateThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_UPDATE, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_UPDATE);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 401 Fail: n/a
	 */
	@Test
	public void test14AnonUpdateThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = "{\"Location\":" + LOCATION + "}";
		HttpPatch httpPatch = new HttpPatch(thingURL);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpPatch);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE);
		}
	}

	/*
	 * DELETE Tests
	 */

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE Success: 200 Fail: n/a
	 */
	@Test
	public void test20SameUserDeleteThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		HttpDelete httpDelete = new HttpDelete(thingURL);

		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200)
		{
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
	 */
	@Test
	public void test21OtherUserDeleteThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		HttpDelete httpDelete = new HttpDelete(thingURL);

		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
		}
	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE Success: 200 Fail: n/a
	 */
	@Test
	public void test22AdminDeleteThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		HttpDelete httpDelete = new HttpDelete(thingURL);

		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			LOGGER.info(ADMIN_SHOULD_BE_ABLE_TO_DELETE);
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 401 Fail: n/a
	 */
	@Test
	public void test23AnonDeleteThing() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		HttpDelete httpDelete = new HttpDelete(thingURL);

		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE);
		}
	}

	
	/*
	 * Location Tests
	 */
	
	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_ADD_LOCATION Success: 201 Fail: n/a
	 */
	@Test
	public void test30SameUserAddThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_ADD_LOCATION, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_ADD_LOCATION);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION Success: 403 Fail: n/a
	 */
	@Test
	public void test31OtherUserAddThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION);
		}
	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_ADD_LOCATION Success: 201 Fail: n/a
	 */
	@Test
	public void test32AdminAddThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_ADD_LOCATION, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_ADD_LOCATION);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION Success: 401 Fail: n/a
	 */
	@Test
	public void test33AnonAddThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = LOCATION;
		HttpPost httpPost = new HttpPost(thingURL + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_ADD_LOCATION);
		}
	}

	private void addLocation(String thingUrl, String userId, int obsId) throws IOException
	{
		String request = String.format(LOCATION_ID, obsId);
		HttpPost httpPost = new HttpPost(thingUrl + "/Locations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, userId, "");
		CloseableHttpResponse response = service.execute(httpPost);

	}

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_LOCATION Success: 200 Fail: n/a
	 */
	@Test
	public void test40SameUserDeleteThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addLocation(thingURL, LJS, ++locationId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Locations(" + locationId + ")");
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_DELETE_LOCATION, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_LOCATION);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION Success: 403 Fail: n/a
	 */
	@Test
	public void test41OtherUserDeleteThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addLocation(thingURL, LJS, ++locationId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Locations(" + locationId + ")");
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION);
		}
	}
	
	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE_LOCATION Success: 200 Fail: n/a
	 */
	@Test
	public void test42AdminDeleteThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addLocation(thingURL, LJS, ++locationId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Locations(" + locationId + ")");
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_DELETE_LOCATION, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE_LOCATION);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION Success: 401 Fail: n/a
	 */
	@Test
	public void test43AnonDeleteThingLocation() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addLocation(thingURL, LJS, ++locationId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Locations(" + locationId + ")");
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_LOCATION);
		}
	}
	
	/*
	 * Datastream Tests
	 */
	
	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_ADD_DATASTREAM Success: 201 Fail: n/a
	 */
	@Test
	public void test50SameUserAddThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = String.format(DATASTREAM_PARTY,LJS, LJS);
		HttpPost httpPost = new HttpPost(thingURL + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_ADD_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_ADD_DATASTREAM);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM Success: 403 Fail: n/a
	 */
	@Test
	public void test51OtherUserAddThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = String.format(DATASTREAM_PARTY,LJS, LJS);
		HttpPost httpPost = new HttpPost(thingURL + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM);
		}
	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_ADD_DATASTREAM Success: 201 Fail: n/a
	 */
	@Test
	public void test52AdminAddThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = String.format(DATASTREAM_PARTY,LJS, LJS);
		HttpPost httpPost = new HttpPost(thingURL + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_ADD_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_ADD_DATASTREAM);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM Success: 401 Fail: n/a
	 */
	@Test
	public void test53AnonAddThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		
		String request = String.format(DATASTREAM_PARTY,LJS, LJS);
		HttpPost httpPost = new HttpPost(thingURL + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_ADD_DATASTREAM);
		}
	}

	private void addDatastream(String thingUrl, String userId, int dsId) throws IOException
	{
		String request = String.format(DATASTREAM_PARTY_ID, dsId, userId, userId);
		HttpPost httpPost = new HttpPost(thingUrl + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, userId, "");
		CloseableHttpResponse response = service.execute(httpPost);

	}

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM Success: 200 Fail: n/a
	 */
	@Test
	public void test60SameUserDeleteThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addDatastream(thingURL, LJS, ++datastreamId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Datastreams(" + datastreamId + ")");
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM Success: 403 Fail: n/a
	 */
	@Test
	public void test61OtherUserDeleteThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addDatastream(thingURL, LJS, ++datastreamId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Datastreams(" + datastreamId + ")");
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403) {
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM);
		}
	}
	
	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM Success: 200 Fail: n/a
	 */
	@Test
	public void test62AdminDeleteThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addDatastream(thingURL, LJS, ++datastreamId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Datastreams(" + datastreamId + ")");
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_DELETE_DATASTREAM);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM Success: 401 Fail: n/a
	 */
	@Test
	public void test63AnonDeleteThingDatastream() throws ClientProtocolException, IOException {
		String thingURL = createThingParty(LJS);
		addDatastream(thingURL, LJS, ++datastreamId);
		
		HttpDelete httpDelete = new HttpDelete(thingURL + "/Datastreams(" + datastreamId + ")");
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE_DATASTREAM);
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
		Assert.fail(assertion);
	}

	private String createParty(String userId) throws IOException
	{
		String request = String.format(PARTY,userId);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, userId, "");
		CloseableHttpResponse response = service.execute(httpPost);

		return response.getFirstHeader("Location").getValue();
	}
	
}
