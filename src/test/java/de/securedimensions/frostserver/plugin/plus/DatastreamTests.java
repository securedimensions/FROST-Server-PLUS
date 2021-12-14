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
 * Tests for the Datastream class properties. According to the ownership concept, a
 * Datastream's properties can only be changed by the user that 'owns' the Datastream
 * instance. 
 *
 * @author Andreas Matheus
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatastreamTests extends AbstractTestClass {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(DatastreamTests.class);

	private static final String ADMIN_SHOULD_BE_ABLE_TO_CREATE = "Admin should be able to create.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_UPDATE = "Admin should be able to update.";
	private static final String ADMIN_SHOULD_BE_ABLE_TO_DELETE = "Admin should be able to delete.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_CREATE_ASSOCIATED = "Same user should be able to create Datastream associated to own Party.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE = "Same user should be able to update.";
	private static final String SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_OBSERVATION = "Same user should be able to update Observation.";
	private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY = "Same user should be able to update Party.";
	private static final String SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Same User should NOT be able to delete.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_ASSOCIATED = "Other user should NOT be able to create Datastream associated to other Party.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE = "Other user should NOT be able to update.";
	private static final String OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE = "Other user should NOT be able to delete.";
	private static final String ANY_USER_SHOULD_BE_ABLE_TO_CREATE_UNLINKED = "Any user should be able to create Datastream NOT linked to a Party.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_CREATE = "anon should NOT be able to create.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_UPDATE = "anon should NOT be able to update.";
	private static final String ANON_SHOULD_NOT_BE_ABLE_TO_DELETE = "anon should NOT be able to delete.";

	public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
	public static final String LJS = "21232f29-7a57-35a7-8389-4a0e4a801fc3";
	public static final String ADMIN = "admin";

	private static final String  DATASTREAM = "{\n"
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
			+ "    \"Thing\": {\n"
			+ "         \"name\": \"Raspberry Pi 4 B, 4x 1,5 GHz, 4 GB RAM, WLAN, BT\",\n"
			+ "        \"description\": \"Raspberry Pi 4 Model B is the latest product in the popular Raspberry Pi range of computers\",\n"
			+ "        \"properties\": {\n"
			+ "            \"CPU\": \"1.4GHz\",\n"
			+ "            \"RAM\": \"4GB\"\n"
			+ "        }\n"
			+ "    }\n"
			+ "}";
	
	private static String PARTY_ALICE = String.format("{\"name\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
	private static String PARTY_LJS = String.format("{\"name\": \"Long John Silver Citizen Scientist\", \"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);
	
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
			+ "        }\n"
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
	}

	private static SensorThingsService service;

	private String partyLJS, partyALICE;

	public DatastreamTests(ServerVersion version) throws ServiceFailureException, IOException, URISyntaxException {
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
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE_ASSOCIATED Success: 201 Fail: n/a
	 */
	@Test
	public void test01SameUserCreateDatastreamsAssoc() throws ClientProtocolException, IOException {
		String request = String.format(DATASTREAM_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
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
			
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_CREATE_ASSOCIATED,
					map.get("authId").equalsIgnoreCase(LJS));
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE_ASSOCIATED);
		}

	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_ASSOCIATED Success: 403 Fail: 201
	 */
	@Test
	public void test02OtherUserCreateDatastreamAssoc() throws ClientProtocolException, IOException {
		String request = String.format(DATASTREAM_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_ASSOCIATED, Boolean.TRUE);
		} else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertFalse(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_ASSOCIATED, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE_ASSOCIATED);
		}

	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
	 */
	@Test
	public void test03AdminCreateDatastreamAssoc() throws ClientProtocolException, IOException {
		String request = String.format(DATASTREAM_PARTY,ALICE);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			// we need to make sure that the Datastream is associated with Alice
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
	public void test02AnonCreateDatastreamAssoc() throws ClientProtocolException, IOException {
		String request = String.format(DATASTREAM_PARTY,LJS);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
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
	 * ANY_USER_SHOULD_BE_ABLE_TO_CREATE_UNLINKED Success: 201 Fail: n/a
	 */
	@Test
	public void test01AnyeUserCreateDatastream() throws ClientProtocolException, IOException {
		String request = DATASTREAM;
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(ANY_USER_SHOULD_BE_ABLE_TO_CREATE_UNLINKED, true);
		} else {
			fail(response, ANY_USER_SHOULD_BE_ABLE_TO_CREATE_UNLINKED);
		}

	}

	/*
	 * UPDATE Tests
	 */

	private String createDatastreamParty(String user) throws IOException
	{
		String request = String.format(DATASTREAM_PARTY,user);
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Datastreams");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		return response.getFirstHeader("Location").getValue();
	}
	
	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateDatastream() throws ClientProtocolException, IOException {
		String thingURL = createDatastreamParty(LJS);
		
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
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_OBSERVATION Success: 200 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateDatastreamObservation() throws ClientProtocolException, IOException {
		String request = OBSERVATION;
		HttpPost httpPost = new HttpPost(createDatastreamParty(LJS) + "/Observations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_OBSERVATION, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_UPDATE_OBSERVATION);
		}
	}

	/*
	 * SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_PARTY Success: 200 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateDatastreamParty() throws ClientProtocolException, IOException {
		String thingURL = createDatastreamParty(LJS);
		
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
	public void test12OtherUserUpdateDatastreamObservation() throws ClientProtocolException, IOException {
		String request = OBSERVATION;
		HttpPost httpPost = new HttpPost(createDatastreamParty(LJS) + "/Observations");
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
	public void test13AdminUpdateDatastreamObservation() throws ClientProtocolException, IOException {
		String request = OBSERVATION;
		HttpPost httpPost = new HttpPost(createDatastreamParty(LJS) + "/Observations");
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
	public void test14AnonUpdateParty() throws ClientProtocolException, IOException {
		String request = "{\"Observation\":" + OBSERVATION + "}";
		HttpPatch httpPatch = new HttpPatch(createDatastreamParty(LJS));
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
	 * SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
	 */
	@Test
	public void test20SameUserDeleteParty() throws ClientProtocolException, IOException {
		HttpDelete httpDelete = new HttpDelete(partyLJS);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_DELETE);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_DELETE Success: 403 Fail: n/a
	 */
	@Test
	public void test21OtherUserDeleteParty() throws ClientProtocolException, IOException {
		HttpDelete httpDelete = new HttpDelete(partyALICE);
		setAuth(service, LJS, "");
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
	public void test22AdminDeleteParty() throws ClientProtocolException, IOException {
		// Party Alice created by Admin in a previous test
		HttpDelete httpDelete = new HttpDelete(partyALICE);
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
	public void test23AnonDeleteParty() throws ClientProtocolException, IOException {
		HttpDelete httpDelete = new HttpDelete(partyALICE);
		unsetAuth(service);
		CloseableHttpResponse response = service.execute(httpDelete);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_401) {
			Assert.assertTrue(ANON_SHOULD_NOT_BE_ABLE_TO_DELETE, Boolean.TRUE);
		} else {
			fail(response, ANON_SHOULD_NOT_BE_ABLE_TO_DELETE);
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

	
}