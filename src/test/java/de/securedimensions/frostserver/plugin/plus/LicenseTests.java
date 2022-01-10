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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
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
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.yaml.snakeyaml.DumperOptions.Version;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.statests.AbstractTestClass;
import de.fraunhofer.iosb.ilt.statests.ServerVersion;
import de.securedimensions.frostserver.plugin.plus.auth.PrincipalAuthProvider;
import de.securedimensions.frostserver.plugin.plus.helper.TableHelperObservation;

/**
 * Tests for the Group class properties. According to the ownership concept, a
 * Group's properties can only be changed by the user that 'owns' the Group
 * instance. That user has the same UUID as the Party's authId property.
 *
 * @author Andreas Matheus
 */
public class LicenseTests extends AbstractTestClass {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(LicenseTests.class);

    private static final long serialVersionUID = 1639739965;


	public static final String ALICE = "505851c3-2de9-4844-9bd5-d185fe944265";
	public static final String ADMIN = "admin";

	private static String PARTY_ALICE = String.format("{\"name\": \"Alice in Wonderland\", \"description\": \"The young girl that fell through a rabbit hole into a fantasy world of anthropomorphic creatures\", \"displayName\": \"ALICE\", \"role\": \"individual\", \"authId\": \"%s\"}", ALICE);

	private static String GROUP(String license) { return String.format("{\n"
			+ "	\"name\": \"Group\",\n"
			+ "	\"description\": \"with license\",\n"
			+ "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
			+ "    \"Party\": " + PARTY_ALICE + ",\n"
			+ "    \"License\": %s\n"
			+ "}",license);}

	private static String GROUP(int id, String license) { return String.format("{\n"
			+ " \"@iot.id\": %d,\n"
			+ "	\"name\": \"Group\",\n"
			+ "	\"description\": \"with license\",\n"
			+ "  \"creationTime\": \"2021-12-12T12:12:12Z\",\n"
			+ "    \"Party\": " + PARTY_ALICE + ",\n"
			+ "    \"License\": %s\n"
			+ "}",id, license);}
	
	private static String DATASTREAM(String license) { return String.format("{\n"
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
			+ "}", license);}
	
	private static String DATASTREAM(int id, String license) { return String.format("{\n"
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
			+ "}", id, license);}
	
	protected static final String OBSERVATION_DATASTREAM(String license) { return String.format("{\n"
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
			+ "}", DATASTREAM(license));}
	
	protected static final String OBSERVATION_DATASTREAM(int id, String license) { return String.format("{\n"
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
			+ "}", DATASTREAM(id, license));}
	
	protected static final String OBSERVATION_GROUP(String datastream, String license) { return String.format("{\n"
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
			+ "    \"Groups\": [%s],\n"
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
			+ "}", datastream, GROUP(license));}
	
	
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
		SERVER_PROPERTIES.put("plugins.plus.enable.enforceLicensing", true);
		SERVER_PROPERTIES.put("auth.provider", PrincipalAuthProvider.class.getName());
		// For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
		SERVER_PROPERTIES.put("auth.allowAnonymousRead", "true");
		// For the moment we need to use ServerAndClient until FROST-Server supports to deactivate per Entityp
		SERVER_PROPERTIES.put("persistence.idGenerationMode", "ServerAndClientGenerated");
		SERVER_PROPERTIES.put("plugins.coreModel.idType","LONG");
		//SERVER_PROPERTIES.put("plugins.plus.idType.group", "String");
		SERVER_PROPERTIES.put("plugins.plus.idType.license", "String");
		//SERVER_PROPERTIES.put("plugins.coreModel.idType.datastream", "String");
		//SERVER_PROPERTIES.put("plugins.multiDatastream.idType.multiDatastream", "String");
		SERVER_PROPERTIES.put("plugins.multiDatastream.enable", true);
		

	}

	public LicenseTests(ServerVersion version) throws ServiceFailureException, IOException, URISyntaxException {
		super(version, SERVER_PROPERTIES);
	}

	@Override
	protected void setUpVersion() {
		LOGGER.info("Setting up for version {}.", version.urlPart);
		try {
			service = new SensorThingsService(new URL(serverSettings.getServiceUrl(version)));
			
			for (String k : TableHelperObservation.LICENSES.keySet())
			{
				createEntity("/Licenses", TableHelperObservation.LICENSES.get(k));
			}

			
		} catch (IOException ex) {
			LOGGER.error("Failed to execute request", ex);
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

	private int createEntity(String path, String request) throws IOException
	{
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + path);
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ADMIN, "");
		CloseableHttpResponse response = service.execute(httpPost);
		
		if (response.getStatusLine().getStatusCode() != 201)
			LOGGER.error(org.apache.http.util.EntityUtils.toString(response.getEntity()));
		
		return response.getStatusLine().getStatusCode();
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

	private static void setAuth(SensorThingsService service, String username, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		URL url = service.getEndpoint();

		credsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
				new UsernamePasswordCredentials(username, password));

		service.setHttpClient(HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build());
	}

	  public static List<Object[]> data() {
		    return Arrays.asList(new Object[][] { 
		    	// Datastream.License, Group.License, status code
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_PD_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_SA_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_NC_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_ND_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_NC_SA_ID, 201},
		    	{TableHelperObservation.CC_PD_ID, TableHelperObservation.CC_BY_NC_ND_ID, 201},
		    	
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_ID, 201},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_SA_ID, 201},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_NC_ID, 201},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_NC_SA_ID, 201},
		    	{TableHelperObservation.CC_BY_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},
		    	
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_ID, 201},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_SA_ID, 201},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_NC_ID, 400},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_NC_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_SA_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},

		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_NC_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_NC_SA_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},
		    	
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_NC_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_NC_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_ND_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},
		    	
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_NC_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_NC_SA_ID, 201},
		    	{TableHelperObservation.CC_BY_NC_SA_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},
		    	

		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_PD_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_NC_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_ND_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_NC_SA_ID, 400},
		    	{TableHelperObservation.CC_BY_NC_ND_ID, TableHelperObservation.CC_BY_NC_ND_ID, 400},
		    	
		    });
		  }

		@Test
		public void testIdId() throws ClientProtocolException, IOException
		{
			for (Object[] d : data())
			{
				String datastreamLicenseId = (String)d[0];
				String groupLicenseId = (String)d[1];
				int expectedStatus = (int)d[2];
				
				int actualStatus = testIdId(datastreamLicenseId, groupLicenseId);
				String test = "TestIdId: Datastream.License(@iot.id=" + datastreamLicenseId + ") - Group.License(@iot.id=" + groupLicenseId + ")";
				LOGGER.info(test);
				Assert.assertTrue(test, expectedStatus == actualStatus);
				if (expectedStatus != actualStatus)
					LOGGER.info("FAIL: " + test + " expected status code: {} - actual status code: {}", expectedStatus, actualStatus);
			}
		}
		
		
	private int testIdId(String datastreamLicenseId, String groupLicenseId) throws IOException 
	{
		String datastreamLicense = "{\"@iot.id\": \"" + datastreamLicenseId + "\"}"; 
		String groupLicense = "{\"@iot.id\": \"" + groupLicenseId + "\"}"; 
		String request = OBSERVATION_GROUP(DATASTREAM(datastreamLicense), groupLicense); 
		return createObservation(request);

	}
		
	private int createObservation(String request) throws IOException
	{
		HttpPost httpPost = new HttpPost(serverSettings.getServiceUrl(version) + "/Observations");
		HttpEntity stringEntity = new StringEntity(request, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		setAuth(service, ALICE, "");

		CloseableHttpResponse response = service.execute(httpPost);
		
		if (response.getStatusLine().getStatusCode() != 201)
			LOGGER.error(org.apache.http.util.EntityUtils.toString(response.getEntity()));
		
		return response.getStatusLine().getStatusCode();

	}
	
}