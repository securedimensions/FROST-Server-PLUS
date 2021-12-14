package de.securedimensions.frostserver.plugin.plus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.statests.AbstractTestClass;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.fraunhofer.iosb.ilt.statests.util.EntityUtils;

import de.securedimensions.frostserver.plugin.plus.auth.PrincipalAuthProvider;
import de.securedimensions.frostserver.plugin.plus.PluginPLUS;

/**
 * Tests for the Party class properties. According to the ownership concept, a
 * Party's properties can only be changed by the user that 'owns' the Party
 * instance. That user has the same UUID as the Party's authId property.
 *
 * @author Andreas Matheus
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PartyCRUDTests extends AbstractTestClass {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PartyCRUDTests.class);

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
	
	public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
	public static final String LJS = "21232f29-7a57-35a7-8389-4a0e4a801fc3";
	public static final String ADMIN = "admin";

	private static String PARTY_ALICE = String.format("{\"name\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"displayName\": \"ALICE\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);
	private static String PARTY_LJS = String.format("{\"name\": \"Long John Silver Citizen Scientist\", \"description\": \"The opportunistic pirate by Robert Louis Stevenson\", \"displayName\": \"LJS\", \"role\": \"individual\", \"authId\": \"%s\"}", LJS);

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

	private String partyLJSUrl, partyALICEUrl;

	public PartyCRUDTests(ServerVersion version) throws ServiceFailureException, IOException, URISyntaxException {
		super(version, SERVER_PROPERTIES);
		// This is the party that we are going to apply the Update and Delete tests
		// on...
		partyLJSUrl = serverSettings.getServiceUrl(version) + "/Parties('" + LJS + "')";
		partyALICEUrl = serverSettings.getServiceUrl(version) + "/Parties('" + ALICE + "')";
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
	 * SAME_USER_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
	 */
	@Test
	public void test01SameUserCreateParty() throws ClientProtocolException, IOException {
		String request = PARTY_LJS;
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			String location = response.getFirstHeader("Location").getValue();
			Boolean sucess = (location != null) && location.contains(LJS);
			Assert.assertTrue(SAME_USER_SHOULD_BE_ABLE_TO_CREATE, sucess);
		} else {
			fail(response, SAME_USER_SHOULD_BE_ABLE_TO_CREATE);
		}

	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 403 Fail: 201
	 */
	@Test
	public void test02OtherUserCreateParty() throws ClientProtocolException, IOException {
		String request = PARTY_LJS;
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_400)
		{
			Assert.assertTrue(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE, Boolean.TRUE);
		} else if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			Assert.assertFalse(OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE, Boolean.TRUE);
		} else {
			fail(response, OTHER_USER_SHOULD_NOT_BE_ABLE_TO_CREATE);
		}

	}

	/*
	 * ADMIN_SHOULD_BE_ABLE_TO_CREATE Success: 201 Fail: n/a
	 */
	@Test
	public void test03AdminCreateParty() throws ClientProtocolException, IOException {
		String request = PARTY_ALICE;
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_201) {
			// we need to make sure that Alice was created
			Assert.assertTrue(ADMIN_SHOULD_BE_ABLE_TO_CREATE,
					response.getFirstHeader("Location").getValue().equalsIgnoreCase(partyALICEUrl));
		} else {
			fail(response, ADMIN_SHOULD_BE_ABLE_TO_CREATE);
		}
	}

	/*
	 * ANON_SHOULD_NOT_BE_ABLE_TO_CREATE Success: 401 Fail: 201
	 */
	@Test
	public void test02AnonCreateParty() throws ClientProtocolException, IOException {
		String request = PARTY_LJS;
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Parties");
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

	/*
	 * SAME_USER_SHOULD_BE_ABLE_TO_UPDATE Success: 200 Fail: n/a
	 */
	@Test
	public void test10SameUserUpdateParty() throws ClientProtocolException, IOException {
		String request = "{\"role\": \"institutional\"}";
		HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
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
	 * SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID Success: 403 Fail: n/a
	 */
	@Test
	public void test11SameUserUpdatePartyAuthId() throws ClientProtocolException, IOException {
		String request = "{\"authId\": \"505851c3-2de9-4844-9bd5-d185fe944265\"}";
		HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		setAuth(service, LJS, "");
		CloseableHttpResponse response = service.execute(httpPatch);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_403)
		{
			Assert.assertTrue(SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID, Boolean.TRUE);
		} else {
			fail(response, SAME_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE_AUTHID);
		}
	}

	/*
	 * OTHER_USER_SHOULD_NOT_BE_ABLE_TO_UPDATE Success: 403 Fail: n/a
	 */
	@Test
	public void test12OtherUserUpdateParty() throws ClientProtocolException, IOException {
		String request = "{\"role\": \"institutional\"}";
		HttpPatch httpPatch = new HttpPatch(partyLJSUrl);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		setAuth(service, ALICE, "");
		CloseableHttpResponse response = service.execute(httpPatch);

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
	public void test13AdminUpdateParty() throws ClientProtocolException, IOException {
		String request = "{\"role\": \"institutional\"}";
		HttpPatch httpPatch = new HttpPatch(partyALICEUrl);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPatch.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPatch);

		if (response.getStatusLine().getStatusCode() == HTTP_CODE_200) {
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
		String request = "{\"role\": \"institutional\"}";
		HttpPatch httpPatch = new HttpPatch(partyALICEUrl);
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
		HttpDelete httpDelete = new HttpDelete(partyLJSUrl);
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
		HttpDelete httpDelete = new HttpDelete(partyALICEUrl);
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
		HttpDelete httpDelete = new HttpDelete(partyALICEUrl);
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
		HttpDelete httpDelete = new HttpDelete(partyALICEUrl);
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
