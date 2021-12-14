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

import com.fasterxml.jackson.core.type.TypeReference;
import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInterval;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntity;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntitySet;
import static de.fraunhofer.iosb.ilt.frostserver.property.SpecialNames.AT_IOT_ID;

import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeComplex;
import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeSimpleCustom;
import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeSimplePrimitive;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginModel;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginRootDocument;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.util.LiquibaseUser;
import de.fraunhofer.iosb.ilt.frostserver.util.PrincipalExtended;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdUuid;
import java.io.IOException;
import java.io.Writer;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.UUID;

import org.jooq.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author am
 * @author scf
 */
public class PluginPLUS implements PluginRootDocument, PluginModel, LiquibaseUser {

    private static final String LIQUIBASE_CHANGELOG_FILENAME = "liquibase/plus/tables.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPLUS.class.getName());

    private static final long serialVersionUID = 1626971234;

    /**
     * Class License
     */
    public final EntityPropertyMain<String> epLicenseDefinition = new EntityPropertyMain<>("definition", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<String> epLicenseLogo = new EntityPropertyMain<>("logo", TypeSimplePrimitive.EDM_STRING);

    public final NavigationPropertyEntity npLicenseDatastream = new NavigationPropertyEntity("License");
    public final NavigationPropertyEntitySet npDatastreamsLicense = new NavigationPropertyEntitySet("Datastreams", npLicenseDatastream);

    public final NavigationPropertyEntity npLicenseMultiDatastream = new NavigationPropertyEntity("License");
    public final NavigationPropertyEntitySet npMultiDatastreamsLicense = new NavigationPropertyEntitySet("MultiDatastreams", npLicenseMultiDatastream);

    public final NavigationPropertyEntity npLicenseGroup = new NavigationPropertyEntity("License");
    public final NavigationPropertyEntitySet npGroupsLicense = new NavigationPropertyEntitySet("Groups", npLicenseGroup);

    public final EntityType etLicense = new EntityType("License", "Licenses");

    /**
     * Class Group
     */
    public final EntityPropertyMain<String> epPurpose = new EntityPropertyMain<>("purpose", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<TimeInstant> epGroupCreated = new EntityPropertyMain<>("creationTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, false, true);
    public final EntityPropertyMain<TimeInterval> epGroupRuntime = new EntityPropertyMain<>("runTime", TypeComplex.STA_TIMEINTERVAL, false, true);

    public final EntityPropertyMain<String> epRelationRole = new EntityPropertyMain<>("role", TypeSimplePrimitive.EDM_STRING);

    public final NavigationPropertyEntitySet npObservationGroups = new NavigationPropertyEntitySet("Groups");
    public final NavigationPropertyEntitySet npRelationGroups = new NavigationPropertyEntitySet("Groups");
    public final NavigationPropertyEntitySet npPartyGroups = new NavigationPropertyEntitySet("Parties");
    public final NavigationPropertyEntitySet npObservations = new NavigationPropertyEntitySet("Observations", npObservationGroups);
    public final NavigationPropertyEntitySet npRelations = new NavigationPropertyEntitySet("Relations", npRelationGroups);
    public final NavigationPropertyEntitySet npParties = new NavigationPropertyEntitySet("Parties", npPartyGroups);

    public final EntityType etGroup = new EntityType("Group", "Groups");

    /**
     * Class Relation
     */
    public final EntityPropertyMain<String> epExternalObject = new EntityPropertyMain<>("externalObject", TypeSimplePrimitive.EDM_STRING);

    public final NavigationPropertyEntity npSubjectRelation = new NavigationPropertyEntity("Subject");
    public final NavigationPropertyEntitySet npSubjectsObservation = new NavigationPropertyEntitySet("Subjects", npSubjectRelation);

    public final NavigationPropertyEntity npObjectRelation = new NavigationPropertyEntity("Object");
    public final NavigationPropertyEntitySet npObjectsObservation = new NavigationPropertyEntitySet("Objects", npObjectRelation);

    public final EntityType etRelation = new EntityType("Relation", "Relations");

    /**
     * Class Party
     */
    public enum Role {
        individual, institutional
    };
    public static final TypeReference<Role> TYPE_REFERENCE_ROLE = new TypeReference<Role>() {
        // Empty on purpose.
    };
    public static final TypeSimpleCustom propertyTypeRole = new TypeSimpleCustom("Plus.Role", "The Party Role", TypeSimplePrimitive.EDM_STRING, TYPE_REFERENCE_ROLE);
    public final EntityPropertyMain<Role> epPartyRole = new EntityPropertyMain<>("role", propertyTypeRole);

    public final EntityPropertyMain<String> epAuthId = new EntityPropertyMain<>("authId", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<String> epDisplayName = new EntityPropertyMain<>("displayName", TypeSimplePrimitive.EDM_STRING);

    public final NavigationPropertyEntity npPartyThing = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npThingsParty = new NavigationPropertyEntitySet("Things", npPartyThing);

    public final NavigationPropertyEntity npPartyGroup = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npGroupsParty = new NavigationPropertyEntitySet("Groups", npPartyGroup);

    public final NavigationPropertyEntity npPartyDatastream = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npDatastreamsParty = new NavigationPropertyEntitySet("Datastreams", npPartyDatastream);

    public final NavigationPropertyEntity npPartyMultiDatastream = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npMultiDatastreamsParty = new NavigationPropertyEntitySet("MultiDatastreams", npPartyMultiDatastream);

    public final EntityType etParty = new EntityType("Party", "Parties");

    /**
     * Class Project
     */
    public final EntityPropertyMain<String> epClassification = new EntityPropertyMain<>("classification", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<String> epTermsOfUse = new EntityPropertyMain<>("termsOfUse", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<String> epPrivacyPolicy = new EntityPropertyMain<>("privacyPolicy", TypeSimplePrimitive.EDM_STRING);
    public final EntityPropertyMain<TimeInstant> epProjectCreated = new EntityPropertyMain<>("creationTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, false, true);
    public final EntityPropertyMain<TimeInterval> epProjectRuntime = new EntityPropertyMain<>("runTime", TypeComplex.STA_TIMEINTERVAL, false, true);
    public final EntityPropertyMain<String> epUrl = new EntityPropertyMain<>("url", TypeSimplePrimitive.EDM_STRING);

    public final NavigationPropertyEntity npProjectDatastream = new NavigationPropertyEntity("Project");
    public final NavigationPropertyEntitySet npDatastreamsProject = new NavigationPropertyEntitySet("Datastreams", npProjectDatastream);

    public final NavigationPropertyEntity npProjectMultiDatastream = new NavigationPropertyEntity("Project");
    public final NavigationPropertyEntitySet npMultiDatastreamsProject = new NavigationPropertyEntitySet("MultiDatastreams", npProjectMultiDatastream);

    public final EntityType etProject = new EntityType("Project", "Projects");

    // Type IDs
    public EntityPropertyMain<?> epIdGroup;
    public EntityPropertyMain<?> epIdLicense;
    public EntityPropertyMain<?> epIdParty;
    public EntityPropertyMain<?> epIdProject;
    public EntityPropertyMain<?> epIdRelation;

    private static final List<String> REQUIREMENTS_PLUS = Arrays.asList(
            "https://github.com/securedimensions/FROST-Server-PLUS");

    private CoreSettings settings;
    private PluginPlusSettings modelSettings;
    private boolean enabled;
    private boolean enforceOwnsership;
    private boolean transferOwnsership;
    private boolean fullyInitialised;

    public PluginPLUS() {
        LOGGER.info("Creating new PLUS Plugin.");
    }

    @Override
    public void init(CoreSettings settings) {
        this.settings = settings;
        Settings pluginSettings = settings.getPluginSettings();
        enabled = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_PLUS, PluginPlusSettings.class);
        if (!enabled) {
            return;
        }
        enforceOwnsership = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_ENFORCE_OWNERSHIP, PluginPlusSettings.class);
        transferOwnsership = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_TRANSFER_OWNERSHIP, PluginPlusSettings.class);
        modelSettings = new PluginPlusSettings(settings);
        settings.getPluginManager().registerPlugin(this);

        final PluginCoreModel pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        final ModelRegistry mr = settings.getModelRegistry();
        /**
         * Class License
         */
        epIdLicense = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(modelSettings.idTypeLicense), "id");
        etLicense
                .registerProperty(epIdLicense, false)
                .registerProperty(ModelRegistry.EP_SELFLINK, false)
                .registerProperty(pluginCoreModel.epName, false)
                .registerProperty(pluginCoreModel.epDescription, false)
                .registerProperty(ModelRegistry.EP_PROPERTIES, false)
                .registerProperty(epLicenseDefinition, true)
                .registerProperty(epLicenseLogo, false)
                .registerProperty(npDatastreamsLicense, false)
                .registerProperty(npGroupsLicense, false);

        npLicenseDatastream.setEntityType(etLicense);
        npDatastreamsLicense.setEntityType(pluginCoreModel.etDatastream);
        pluginCoreModel.etDatastream
        		.registerProperty(npLicenseDatastream, false);

        /**
         * Class Party
         */
        epIdParty = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(modelSettings.idTypeParty), "id");
        etParty
                .registerProperty(epIdParty, false)
                .registerProperty(ModelRegistry.EP_SELFLINK, false)
                .registerProperty(pluginCoreModel.epName, false)
                .registerProperty(pluginCoreModel.epDescription, false)
                .registerProperty(epAuthId, false)
                .registerProperty(epDisplayName, false)
                .registerProperty(epPartyRole, true)
                .registerProperty(npThingsParty, false)
		        .registerProperty(npGroupsParty, false)
		        .registerProperty(npDatastreamsParty, false)
                .addValidator((entity, entityPropertiesOnly) -> {
                	
                	if (enforceOwnsership == false)
                		return;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                		return;

                	String userId = principal.getName();
                	
                	if ((entity.isSetProperty(epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty(epAuthId))))
                	{
                		// The authId is set by this plugin - it cannot be different from the POSTed Party property authId
                		throw new IllegalArgumentException("Party property 'authId' must represent the acting user");                    	
                	}
            		try
            		{
            		    // This throws exception if userId is not in UUID format
            			UUID.fromString(userId);
            		    entity.setProperty(epAuthId, userId);
            		} catch (IllegalArgumentException exception)
            		{
            			entity.setProperty(epAuthId, UUID.nameUUIDFromBytes(userId.getBytes()).toString());
            		}        			
                	
                })
                .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
                	
                	if (enforceOwnsership == false)
                		return;
                	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if (isAdmin(principal))
                	{
                		// An admin can override the authId of any Party
                		String authId = entity.getProperty(epAuthId);
                		if (authId == null)
                			return;
                		
                		// This throws exception if POSTed authId is not in UUID format
            			UUID.fromString(authId);
            			return;
                	}
                	
                	String userId = principal.getName();
                	if ((entity.isSetProperty(epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty(epAuthId))))
                	{
                		// The authId is set by the plugin - it cannot be changed via a PATCH
                		throw new ForbiddenException("Party property 'authId' cannot be changed");
                	}
                	
                });

        /**
         * Class Thing
         */
        npPartyThing.setEntityType(etParty);
        pluginCoreModel.etThing
	        .registerProperty(npPartyThing, false)
	        .addValidator((entity, entityPropertiesOnly) -> {
	        	
	        	if (enforceOwnsership == false)
	        		return;
	        	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

        		if (isAdmin(principal))
        			return;
        		
        		Entity party = entity.getProperty(npPartyThing);
        		if (party != null)
        			assertOwnership(entity, party, principal);

	        })
	        .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
	        	
	        	if (enforceOwnsership == false)
	        		return;
	        	
        		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

        		if (isAdmin(principal))
        			return;
	
        		Entity party = entity.getProperty(npPartyThing);
        		if (party != null)
        			assertOwnership(entity, party, principal);
        		else
        			assertPrincipal(principal);

	        });

        npThingsParty.setEntityType(pluginCoreModel.etThing);
        
        npPartyGroup.setEntityType(etParty);
        etGroup.registerProperty(npPartyGroup, false);
        npGroupsParty.setEntityType(etGroup);
        
        /**
         * Class Datastream
         */
        npPartyDatastream.setEntityType(etParty);
        pluginCoreModel.etDatastream
        	.registerProperty(npPartyDatastream, false)
        	 .addValidator((entity, entityPropertiesOnly) -> {
 	        	
 	        	if (enforceOwnsership == false)
 	        		return;
 	        	
         		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

         		if (isAdmin(principal))
         			return;
         		
         		Entity party = entity.getProperty(npPartyDatastream);
         		if (party != null)
         			assertOwnership(entity, party, principal);

 	        })
 	        .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
 	        	
 	        	if (enforceOwnsership == false)
 	        		return;
 	        	
         		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

         		if (isAdmin(principal))
         			return;
 	
         		Entity party = entity.getProperty(npPartyDatastream);
         		if (party != null)
         			assertOwnership(entity, party, principal);
         		else
         			assertPrincipal(principal);

 	        });
        	
        npDatastreamsParty.setEntityType(pluginCoreModel.etDatastream);


        /**
         * Class Project
         */
        epIdProject = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(modelSettings.idTypeProject), "id");
        etProject
                .registerProperty(epIdProject, false)
                .registerProperty(ModelRegistry.EP_SELFLINK, false)
                .registerProperty(pluginCoreModel.epName, false)
                .registerProperty(pluginCoreModel.epDescription, false)
                .registerProperty(ModelRegistry.EP_PROPERTIES, false)
                .registerProperty(epClassification, false)
                .registerProperty(epTermsOfUse, true)
                .registerProperty(epPrivacyPolicy, false)
                .registerProperty(epProjectCreated, true)
                .registerProperty(epProjectRuntime, false)
                .registerProperty(epUrl, false)
                .registerProperty(npDatastreamsProject, false);

        npProjectDatastream.setEntityType(etProject);
        npDatastreamsProject.setEntityType(pluginCoreModel.etDatastream);
        pluginCoreModel.etDatastream.registerProperty(npProjectDatastream, false);

        /**
         * Class Group
         */
        epIdGroup = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(modelSettings.idTypeGroup), "id");
        etGroup
                .registerProperty(epIdGroup, false)
                .registerProperty(ModelRegistry.EP_SELFLINK, false)
                .registerProperty(pluginCoreModel.epName, false)
                .registerProperty(pluginCoreModel.epDescription, false)
                .registerProperty(ModelRegistry.EP_PROPERTIES, false)
                .registerProperty(epPurpose, false)
                .registerProperty(epGroupCreated, true)
                .registerProperty(epGroupRuntime, false)
                .registerProperty(npObservations, false)
                .registerProperty(npRelations, false)
                .registerProperty(npLicenseGroup, false)
                .registerProperty(npPartyGroup, false)
                .addValidator((entity, entityPropertiesOnly) -> {
    	        	
    	        	if (enforceOwnsership == false)
    	        		return;
    	        	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            		if (isAdmin(principal))
                		return;
    	
    	        	assertOwnership(entity, entity.getProperty(npPartyGroup), principal);

    	        })
    	        .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
    	        	
    	        	if (enforceOwnsership == false)
    	        		return;
    	        	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            		if (isAdmin(principal))
                		return;
    	
    	        	assertOwnership(entity, entity.getProperty(npPartyGroup), principal);

    	        });

        npLicenseGroup.setEntityType(etLicense);
        npGroupsLicense.setEntityType(etGroup);

        npObservationGroups.setEntityType(etGroup);
        pluginCoreModel.etObservation.registerProperty(npObservationGroups, false);

        /**
         * Class Relation
         */
        epIdRelation = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(modelSettings.idTypeRelation), "id");
        etRelation
                .registerProperty(epIdRelation, false)
                .registerProperty(ModelRegistry.EP_SELFLINK, false)
                .registerProperty(pluginCoreModel.epName, false)
                .registerProperty(pluginCoreModel.epDescription, false)
                .registerProperty(ModelRegistry.EP_PROPERTIES, false)
                .registerProperty(epRelationRole, false)
                .registerProperty(epExternalObject, false)
                .registerProperty(npSubjectRelation, true)
                .registerProperty(npObjectRelation, false)
                .registerProperty(npRelationGroups, false);

        npSubjectRelation.setEntityType(pluginCoreModel.etObservation);
        npSubjectsObservation.setEntityType(etRelation);
        
        npObjectRelation.setEntityType(pluginCoreModel.etObservation);
        npObjectsObservation.setEntityType(etRelation);

        npRelationGroups.setEntityType(etGroup);
        npRelations.setEntityType(etRelation);

        final PluginMultiDatastream pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);

        // link subject and object to Relations
        pluginCoreModel.etObservation
        		.registerProperty(npSubjectsObservation, false)
        		.registerProperty(npObjectsObservation, false)
     	        .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
    	        	
    	        	if (enforceOwnsership == false)
    	        		return;
    	        	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                	if(isAdmin(principal))
                		return;
    	
                	Entity datastream = entity.getProperty(pluginCoreModel.npDatastreamObservation);
                	if (datastream != null)
                		assertOwnership(entity, datastream.getProperty(npPartyDatastream), principal);

                	if (pluginMultiDatastream != null)
                	{
                		Entity multiDatastream = entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation);
                    	if (multiDatastream != null)
                    		assertOwnership(entity, multiDatastream.getProperty(npPartyDatastream), principal);
                	}

    	        });

        if ((pluginMultiDatastream != null) && pluginMultiDatastream.isEnabled()) {
            /**
             * Class License
             */
            npLicenseMultiDatastream.setEntityType(etLicense);
            npMultiDatastreamsLicense.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream.registerProperty(npLicenseMultiDatastream, false);

            etLicense.registerProperty(npMultiDatastreamsLicense, false);

            /**
             * Class Party
             */
            npPartyMultiDatastream.setEntityType(etParty);
            npMultiDatastreamsParty.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream
            	.registerProperty(npPartyMultiDatastream, false)
    	        .addValidator((entity, entityPropertiesOnly) -> {
    	        	
    	        	if (enforceOwnsership == false)
    	        		return;
    	        	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            		if (isAdmin(principal))
                		return;
    	
    	        	assertOwnership(entity, entity.getProperty(npPartyMultiDatastream), principal);

     	        })
    	        .addValidatorForUpdate((entity, entityPropertiesOnly) -> {
    	        	
    	        	if (enforceOwnsership == false)
    	        		return;
    	        	
            		Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

            		if (isAdmin(principal))
                		return;
    	
    	        	assertOwnership(entity, entity.getProperty(npPartyMultiDatastream), principal);

    	        });


            etParty.registerProperty(npMultiDatastreamsParty, false);

            /**
             * Class Project
             */
            npProjectMultiDatastream.setEntityType(etProject);
            npMultiDatastreamsProject.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream.registerProperty(npProjectMultiDatastream, false);

            etProject.registerProperty(npMultiDatastreamsProject, false);
        }

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isFullyInitialised() {
        return fullyInitialised;
    }

    @Override
    public void modifyServiceDocument(ServiceRequest request, Map<String, Object> result) {
        Map<String, Object> serverSettings = (Map<String, Object>) result.get(Service.KEY_SERVER_SETTINGS);
        if (serverSettings == null) {
            // Nothing to add to.
            return;
        }
        Set<String> extensionList = (Set<String>) serverSettings.get(Service.KEY_CONFORMANCE_LIST);
        extensionList.addAll(REQUIREMENTS_PLUS);
    }

    @Override
    public void registerEntityTypes() {
        LOGGER.info("Initialising PLUS Types...");
        ModelRegistry modelRegistry = settings.getModelRegistry();
        modelRegistry.registerEntityType(etLicense);
        modelRegistry.registerEntityType(etGroup);
        modelRegistry.registerEntityType(etRelation);
        modelRegistry.registerEntityType(etParty);
        modelRegistry.registerEntityType(etProject);
    }

    @Override
    public boolean linkEntityTypes(PersistenceManager pm) {
        LOGGER.info("Linking PLUS Types...");
        final PluginCoreModel pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
    	final PluginMultiDatastream pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);

        if (pluginCoreModel == null || !pluginCoreModel.isFullyInitialised()) {
            return false;
        }

        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            TableCollection tableCollection = ppm.getTableCollection();
            final DataType dataTypeLicense = ppm.getDataTypeFor(modelSettings.idTypeLicense);
            final DataType dataTypeGroup = ppm.getDataTypeFor(modelSettings.idTypeGroup);
            final DataType dataTypeRelation = ppm.getDataTypeFor(modelSettings.idTypeRelation);
            final DataType dataTypeParty = ppm.getDataTypeFor(modelSettings.idTypeParty);
            final DataType dataTypeProject = ppm.getDataTypeFor(modelSettings.idTypeProject);
            final DataType dataTypeObservation = tableCollection.getTableForType(pluginCoreModel.etObservation).getId().getDataType();
            /**
             * Class License
             */
            tableCollection.registerTable(etLicense, new TableImpLicense(dataTypeLicense, this, pluginCoreModel));

            /**
             * Class Group
             */
            tableCollection.registerTable(etGroup, new TableImpGroups(dataTypeGroup, this, pluginCoreModel));
            tableCollection.registerTable(new TableImpGroupsObservations(dataTypeGroup, dataTypeObservation));
            tableCollection.registerTable(new TableImpGroupsRelations(dataTypeGroup, dataTypeRelation));

            /**
             * Class Relation
             */
            tableCollection.registerTable(etRelation, new TableImpRelations(dataTypeRelation, dataTypeObservation, dataTypeGroup, this, pluginCoreModel));

            /**
             * Class Party
             */
            tableCollection.registerTable(etParty, new TableImpParty(dataTypeParty, this, pluginCoreModel, pluginMultiDatastream));

            /**
             * Class Project
             */
            tableCollection.registerTable(etProject, new TableImpProject(dataTypeProject, this, pluginCoreModel));
        }
        fullyInitialised = true;
        return true;
    }

    public Map<String, Object> createLiqibaseParams(PostgresPersistenceManager ppm, Map<String, Object> target) {
        if (target == null) {
            target = new LinkedHashMap<>();
        }
        PluginCoreModel pCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        pCoreModel.createLiqibaseParams(ppm, target);
        PluginMultiDatastream pMultiDs = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);
        if (pMultiDs == null) {
            // Create placeholder variables, otherwise Liquibase complains.
            ppm.generateLiquibaseVariables(target, "MultiDatastream", modelSettings.idTypeDefault);
        } else {
            pMultiDs.createLiqibaseParams(ppm, target);
        }
        ppm.generateLiquibaseVariables(target, "Group", modelSettings.idTypeGroup);
        ppm.generateLiquibaseVariables(target, "License", modelSettings.idTypeLicense);
        ppm.generateLiquibaseVariables(target, "Party", modelSettings.idTypeParty);
        ppm.generateLiquibaseVariables(target, "Project", modelSettings.idTypeProject);
        ppm.generateLiquibaseVariables(target, "Relation", modelSettings.idTypeRelation);

        return target;
    }

    @Override
    public String checkForUpgrades() {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            return ppm.checkForUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null));
        }
        return "Unknown persistence manager class";
    }

    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            return ppm.doUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null), out);
        }
        out.append("Unknown persistence manager class");
        return false;
    }

    public boolean isEnforceOwnershipEnabled()
    {
    	return enforceOwnsership;
    }
    public boolean isTransferOwnershipEnabled()
    {
    	return transferOwnsership;
    }
    
    private boolean isAdmin(Principal principal)
    {
    	if (principal == null)
			return false;
			
    	return ((principal instanceof PrincipalExtended) && ((PrincipalExtended)principal).isAdmin());
    	
    }

    private void assertPrincipal(Principal principal)
    {    	
    	if (principal == null)
    		throw new UnauthorizedException("No Principal");
    }
    
    private void assertAdmin(Principal principal)
    {    	
    	assertPrincipal(principal);    	
		
    	if (!isAdmin(principal))
    		throw new UnauthorizedException("User is not admin");
    }

    private void assertOwnership(Entity entity, Entity party, Principal principal)
    {
    	assertPrincipal(principal);    	   
    	
    	if (party != null)
    	{
        	Id partyId = party.getId();
        	String authId = party.getProperty(epAuthId);

        	if ((partyId == null) && (authId == null))
        		throw new IllegalArgumentException("Party not idenified");    

        	if ((authId != null) && (partyId != null))
        	{
        		LOGGER.warn("Party identified by @iot.id and authId - using authId");
        	}
        	
        	if (authId != null)
        		party.setId(new IdUuid (authId));
        	
        	String userId = principal.getName();
        	if (!userId.equalsIgnoreCase(party.getId().toString()))
        	{
        		// The Id of the Party must match the userId
        		// Entity can only be associated to the Party identifying the acting user
        		throw new ForbiddenException(entity.getEntityType().entityName + " can only be associated with the Party identifying the acting user");                    	
        	}
        	
        	
    	}

    }
}
