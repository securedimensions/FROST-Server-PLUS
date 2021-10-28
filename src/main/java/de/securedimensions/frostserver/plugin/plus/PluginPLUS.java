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
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInterval;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TypeReferencesHelper;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.TableImpObservations;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntity;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntitySet;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginModel;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginRootDocument;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValueBoolean;
import de.fraunhofer.iosb.ilt.frostserver.util.LiquibaseUser;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;

import static de.fraunhofer.iosb.ilt.frostserver.model.ext.TypeReferencesHelper.TYPE_REFERENCE_TIMEINSTANT;
import static de.fraunhofer.iosb.ilt.frostserver.model.ext.TypeReferencesHelper.TYPE_REFERENCE_TIMEINTERVAL;

import java.io.IOException;
import java.io.Writer;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.regex.Pattern;

import org.jooq.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author am
 * @author scf
 */
public class PluginPLUS implements PluginRootDocument, PluginModel, ConfigDefaults, LiquibaseUser {

    private static final String LIQUIBASE_CHANGELOG_FILENAME = "liquibase/plus/tables";

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPLUS.class.getName());

    private static final long serialVersionUID = 1626971234;

    /**
     * Class License
     */
    public final EntityPropertyMain<String> epLicenseDefinition = new EntityPropertyMain<>("definition", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<String> epLicenseLogo = new EntityPropertyMain<>("logo", TypeReferencesHelper.TYPE_REFERENCE_STRING);

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
    public final EntityPropertyMain<String> epPurpose = new EntityPropertyMain<>("purpose", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<TimeInstant> epGroupCreated = new EntityPropertyMain<>("created", TYPE_REFERENCE_TIMEINSTANT, false, true);
    public final EntityPropertyMain<TimeInterval> epGroupRuntime = new EntityPropertyMain<>("runtime", TYPE_REFERENCE_TIMEINTERVAL, false, true);

    public final EntityPropertyMain<String> epRelationRole = new EntityPropertyMain<>("role", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<String> epNamespace = new EntityPropertyMain<>("namespace", TypeReferencesHelper.TYPE_REFERENCE_STRING);

    public final NavigationPropertyEntitySet npObservationGroups = new NavigationPropertyEntitySet("Groups");
    public final NavigationPropertyEntitySet npRelationGroups = new NavigationPropertyEntitySet("Groups");
    public final NavigationPropertyEntitySet npObservations = new NavigationPropertyEntitySet("Observations", npObservationGroups);
    public final NavigationPropertyEntitySet npRelations = new NavigationPropertyEntitySet("Relations", npRelationGroups);

    public final EntityType etGroup = new EntityType("Group", "Groups");

    /**
     * Class Relation
     */
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
    public final EntityPropertyMain<String> epAuthId = new EntityPropertyMain<>("authId", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<String> epNickName = new EntityPropertyMain<>("nickName", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<Role> epPartyRole = new EntityPropertyMain<>("role", TYPE_REFERENCE_ROLE);

    public final NavigationPropertyEntity npPartyDatastream = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npDatastreamsParty = new NavigationPropertyEntitySet("Datastreams", npPartyDatastream);

    public final NavigationPropertyEntity npPartyMultiDatastream = new NavigationPropertyEntity("Party");
    public final NavigationPropertyEntitySet npMultiDatastreamsParty = new NavigationPropertyEntitySet("MultiDatastreams", npPartyMultiDatastream);

    public final EntityType etParty = new EntityType("Party", "Parties");

    /**
     * Class Project
     */
    public final EntityPropertyMain<String> epClassification = new EntityPropertyMain<>("classification", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<String> epTermsOfUse = new EntityPropertyMain<>("termsOfUse", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<String> epPrivacyPolicy = new EntityPropertyMain<>("privacyPolicy", TypeReferencesHelper.TYPE_REFERENCE_STRING);
    public final EntityPropertyMain<TimeInstant> epProjectCreated = new EntityPropertyMain<>("created", TYPE_REFERENCE_TIMEINSTANT, false, true);
    public final EntityPropertyMain<TimeInterval> epProjectRuntime = new EntityPropertyMain<>("runtime", TYPE_REFERENCE_TIMEINTERVAL, false, true);
    public final EntityPropertyMain<String> epUrl = new EntityPropertyMain<>("url", TypeReferencesHelper.TYPE_REFERENCE_STRING);

    public final NavigationPropertyEntity npProjectDatastream = new NavigationPropertyEntity("Project");
    public final NavigationPropertyEntitySet npDatastreamsProject = new NavigationPropertyEntitySet("Datastreams", npProjectDatastream);

    public final NavigationPropertyEntity npProjectMultiDatastream = new NavigationPropertyEntity("Project");
    public final NavigationPropertyEntitySet npMultiDatastreamsProject = new NavigationPropertyEntitySet("MultiDatastreams", npProjectMultiDatastream);

    public final EntityType etProject = new EntityType("Project", "Projects");

    
    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_PLUS = "plus.enable";

    private static final List<String> REQUIREMENTS_PLUS = Arrays.asList(
            "https://www.github.com/securedimensions/STA-PLUS");

    private CoreSettings settings;
    private boolean enabled;
    private boolean fullyInitialised;

    public PluginPLUS() {
        LOGGER.info("Creating new PLUS Plugin.");
    }

    @Override
    public void init(CoreSettings settings) {
        this.settings = settings;
        Settings pluginSettings = settings.getPluginSettings();
        enabled = pluginSettings.getBoolean(TAG_ENABLE_PLUS, getClass());
        if (enabled) {
            settings.getPluginManager().registerPlugin(this);
        }
        
        final PluginCoreModel pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);

        /**
         * Class License
         */
        etLicense
        .registerProperty(ModelRegistry.EP_ID, false)
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
        pluginCoreModel.etDatastream.registerProperty(npLicenseDatastream, false);

        
    	/**
    	 * Class Party
    	 */
        etParty
        .registerProperty(ModelRegistry.EP_ID, false)
        .registerProperty(ModelRegistry.EP_SELFLINK, false)
        .registerProperty(pluginCoreModel.epName, false)
        .registerProperty(pluginCoreModel.epDescription, false)
        .registerProperty(ModelRegistry.EP_PROPERTIES, false)
        .registerProperty(epAuthId, false)
        .registerProperty(epNickName, false)
        .registerProperty(epPartyRole, true)
        .registerProperty(npDatastreamsParty, false)
        .registerProperty(npMultiDatastreamsParty, false)
        .addValidator((entity, entityPropertiesOnly) -> {
        	
        	if (entity.isSetProperty(epAuthId))
        		throw new IllegalArgumentException("property authId cannot be submitted with the request");
        	
        	ServiceRequest request = ServiceRequest.LOCAL_REQUEST.get();
        	String userId = (String)request.getAttributeMap().get("sub");
        	if (userId != null)
        	{
        		try{
        		    // This throws exception if userId is not in UUID format
        			UUID.fromString(userId);
        		    entity.setProperty(epAuthId, userId);
        		} catch (IllegalArgumentException exception){
        			entity.setProperty(epAuthId, UUID.nameUUIDFromBytes(userId.getBytes()).toString());
        		}        			
        	}
        	else
        		entity.setProperty(epAuthId, "00000000-0000-0000-0000-000000000000");
        	
        });

        npPartyDatastream.setEntityType(etParty);
        npDatastreamsParty.setEntityType(pluginCoreModel.etDatastream);

        pluginCoreModel.etDatastream.registerProperty(npPartyDatastream, false);

        /**
         * Class Project
         */
        etProject
        .registerProperty(ModelRegistry.EP_ID, false)
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
        .registerProperty(npDatastreamsProject, false)
        .registerProperty(npMultiDatastreamsProject, false);

        npProjectDatastream.setEntityType(etProject);
        npDatastreamsProject.setEntityType(pluginCoreModel.etDatastream);
        
        pluginCoreModel.etDatastream.registerProperty(npProjectDatastream, false);

        /**
         * Class Group
         */
        etGroup
        .registerProperty(ModelRegistry.EP_ID, false)
        .registerProperty(ModelRegistry.EP_SELFLINK, false)
        .registerProperty(pluginCoreModel.epName, false)
        .registerProperty(pluginCoreModel.epDescription, false)
        .registerProperty(ModelRegistry.EP_PROPERTIES, false)
        .registerProperty(epPurpose, false)
        .registerProperty(epGroupCreated, true)
        .registerProperty(epGroupRuntime, false)
        .registerProperty(npObservations, false)
        .registerProperty(npRelations, false)
        .registerProperty(npLicenseGroup, false);

        npLicenseGroup.setEntityType(etLicense);
    	npGroupsLicense.setEntityType(etGroup);

        npObservationGroups.setEntityType(etGroup);
        pluginCoreModel.etObservation.registerProperty(npObservationGroups, false);


        /**
         * Class Relation
         */
        etRelation
        .registerProperty(ModelRegistry.EP_ID, false)
        .registerProperty(ModelRegistry.EP_SELFLINK, false)
        .registerProperty(pluginCoreModel.epName, false)
        .registerProperty(pluginCoreModel.epDescription, false)
        .registerProperty(ModelRegistry.EP_PROPERTIES, false)
        .registerProperty(epRelationRole, false)
        .registerProperty(epNamespace, false)
        .registerProperty(npSubjectRelation, true)
        .registerProperty(npObjectRelation, true)
        .registerProperty(npRelationGroups, false);

        npSubjectRelation.setEntityType(pluginCoreModel.etObservation);
        npSubjectsObservation.setEntityType(etRelation);
        npObjectRelation.setEntityType(pluginCoreModel.etObservation);
        npObjectsObservation.setEntityType(etRelation);

        npRelationGroups.setEntityType(etGroup);
        npRelations.setEntityType(etRelation);

        // link subject and object to Relations
        pluginCoreModel.etObservation.registerProperty(npSubjectsObservation, false);
        pluginCoreModel.etObservation.registerProperty(npObjectsObservation, false);

        
        final PluginMultiDatastream pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);
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
            pluginMultiDatastream.etMultiDatastream.registerProperty(npPartyMultiDatastream, false);
        	
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
        if (pluginCoreModel == null || !pluginCoreModel.isFullyInitialised()) {
            return false;
        }
        
        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            TableCollection tableCollection = ppm.getTableCollection();
            DataType idType = tableCollection.getIdType();
            
            /**
             * Class License
             */
            tableCollection.registerTable(etLicense, new TableImpLicense(idType, this, pluginCoreModel));

			/**
			 * Class Group
			 */
            tableCollection.registerTable(etGroup, new TableImpGroups(idType, this, pluginCoreModel));
            tableCollection.registerTable(new TableImpGroupsObservations<>(idType));
            tableCollection.registerTable(new TableImpGroupsRelations<>(idType));
            
            /**
             * Class Relation
             */
            tableCollection.registerTable(etRelation, new TableImpRelations(idType, this, pluginCoreModel));
            
            /**
             * Class Party
             */
            tableCollection.registerTable(etParty, new TableImpParty(idType, this, pluginCoreModel));
            
            /**
             * Class Project
             */
            tableCollection.registerTable(etProject, new TableImpProject(idType, this, pluginCoreModel));
            
            
            final TableImpObservations tableObservations = (TableImpObservations) tableCollection.getTableForClass(TableImpObservations.class);

        }
        fullyInitialised = true;
        return true;
    }

    @Override
    public String checkForUpgrades() {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            String fileName = LIQUIBASE_CHANGELOG_FILENAME + ppm.getIdManager().getIdClass().getSimpleName() + ".xml";
            return ppm.checkForUpgrades(fileName);
        }
        return "Unknown persistence manager class";
    }

    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof PostgresPersistenceManager) {
            PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
            String fileName = LIQUIBASE_CHANGELOG_FILENAME + ppm.getIdManager().getIdClass().getSimpleName() + ".xml";
            return ppm.doUpgrades(fileName, out);
        }
        out.append("Unknown persistence manager class");
        return false;
    }

}
