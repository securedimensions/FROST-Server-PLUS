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
package de.securedimensions.frostserver.plugin.staplus;

import static de.fraunhofer.iosb.ilt.frostserver.property.SpecialNames.AT_IOT_ID;

import com.fasterxml.jackson.core.type.TypeReference;
import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.PkValue;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.tables.TableCollection;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.plugin.multidatastream.PluginMultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntity;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain.NavigationPropertyEntitySet;
import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeComplex;
import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeEnumeration;
import de.fraunhofer.iosb.ilt.frostserver.property.type.TypeSimplePrimitive;
import de.fraunhofer.iosb.ilt.frostserver.service.InitResult;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginModel;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginRootDocument;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.util.LiquibaseUser;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;
import de.fraunhofer.iosb.ilt.frostserver.util.user.PrincipalExtended;
import de.securedimensions.frostserver.plugin.staplus.helper.*;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.*;
import org.jooq.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author am
 * @author scf
 */
public class PluginPLUS implements PluginRootDocument, PluginModel, LiquibaseUser {

    public static final TypeReference<Role> TYPE_REFERENCE_ROLE = new TypeReference<Role>() {
        // Empty on purpose.
    };
    public static final TypeEnumeration propertyTypeRole = new TypeEnumeration("Plus.Role", "The Party Role", Role.class, TYPE_REFERENCE_ROLE);
    private static final String LIQUIBASE_CHANGELOG_FILENAME = "liquibase/plus/tables.xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPLUS.class.getName());
    private static final long serialVersionUID = 1626971234;
    private static final List<String> REQUIREMENTS_PLUS = List.of(
            "http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/core",
            "http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/create",
            "http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/update",
            "http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/delete");
    private static final String REQUIREMENT_ENFORCE_OWNERSHIP = "https://github.com/securedimensions/FROST-Server-PLUS#EnforceOwnership";
    private static final String REQUIREMENT_ENFORCE_LICENSING = "https://github.com/securedimensions/FROST-Server-PLUS#EnforceLicensing";
    private static final String REQUIREMENT_ENFORCE_GROUP_LICENSING = "https://github.com/securedimensions/FROST-Server-PLUS#EnforceGroupLicensing";
    private static final String REQUIREMENT_AUTH = "http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/authentication";

    public static final List<String> LICENSE_IDS = Arrays.asList(
            "CC_PD", "CC_BY", "CC_BY_NC", "CC_BY_SA", "CC_BY_NC_SA", "CC_BY_ND", "CC_BY_NC_ND");

    /**
     * Class License
     */
    public final EntityPropertyMain<String> epLicenseDescription = new EntityPropertyMain<>("description", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epLicenseDefinition = new EntityPropertyMain<>("definition", TypeSimplePrimitive.EDM_STRING, true, false);
    public final EntityPropertyMain<String> epLicenseLogo = new EntityPropertyMain<>("logo", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epLicenseAttributionText = new EntityPropertyMain<>("attributionText", TypeSimplePrimitive.EDM_STRING, false, true);
    public final NavigationPropertyEntity npLicenseDatastream = new NavigationPropertyEntity("License", false);
    public final NavigationPropertyEntitySet npDatastreamsLicense = new NavigationPropertyEntitySet("Datastreams", npLicenseDatastream);
    public final NavigationPropertyEntity npLicenseMultiDatastream = new NavigationPropertyEntity("License", false);
    public final NavigationPropertyEntitySet npMultiDatastreamsLicense = new NavigationPropertyEntitySet("MultiDatastreams", npLicenseMultiDatastream);
    public final NavigationPropertyEntity npLicenseGroup = new NavigationPropertyEntity("License", false);
    public final NavigationPropertyEntitySet npGroupsLicense = new NavigationPropertyEntitySet("ObservationGroups", npLicenseGroup);
    public final NavigationPropertyEntity npLicenseCampaign = new NavigationPropertyEntity("License", false);
    public final NavigationPropertyEntitySet npCampaignsLicense = new NavigationPropertyEntitySet("Campaigns", npLicenseCampaign);
    public final EntityType etLicense = new EntityType("License", "Licenses");
    /**
     * Class Group
     */
    public final EntityPropertyMain<String> epGroupPurpose = new EntityPropertyMain<>("purpose", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<TimeInstant> epGroupCreationTime = new EntityPropertyMain<>("creationTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, true, false);
    public final EntityPropertyMain<TimeInstant> epGroupEndTime = new EntityPropertyMain<>("endTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, false, true);
    public final EntityPropertyMain<String> epGroupTermsOfUse = new EntityPropertyMain<>("termsOfUse", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epGroupPrivacyPolicy = new EntityPropertyMain<>("privacyPolicy", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<Map<String, Object>> epGroupDataQuality = new EntityPropertyMain<>("dataQuality", TypeComplex.STA_MAP, false, true);
    public final NavigationPropertyEntitySet npObservationGroups = new NavigationPropertyEntitySet("ObservationGroups");
    public final NavigationPropertyEntitySet npObservationsGroup = new NavigationPropertyEntitySet("Observations", npObservationGroups);
    public final NavigationPropertyEntitySet npRelationGroups = new NavigationPropertyEntitySet("ObservationGroups");
    public final NavigationPropertyEntitySet npRelationsGroup = new NavigationPropertyEntitySet("Relations", npRelationGroups);
    public final EntityType etGroup = new EntityType("ObservationGroup", "ObservationGroups");
    /**
     * Class Relation
     */
    public final EntityPropertyMain<String> epRelationDescription = new EntityPropertyMain<>("description", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epExternalObject = new EntityPropertyMain<>("externalResource", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epRelationRole = new EntityPropertyMain<>("role", TypeSimplePrimitive.EDM_STRING, true, false);
    public final NavigationPropertyEntity npSubjectRelation = new NavigationPropertyEntity("Subject", true);

    public final NavigationPropertyEntitySet npObjectsObservation = new NavigationPropertyEntitySet("Objects", npSubjectRelation);
    public final NavigationPropertyEntity npObjectRelation = new NavigationPropertyEntity("Object", false);
    public final NavigationPropertyEntitySet npSubjectsObservation = new NavigationPropertyEntitySet("Subjects", npObjectRelation);
    public final EntityType etRelation = new EntityType("Relation", "Relations");
    public final EntityPropertyMain<String> epPartyDescription = new EntityPropertyMain<>("description", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<Role> epPartyRole = new EntityPropertyMain<>("role", propertyTypeRole, true, false);
    public final EntityPropertyMain<String> epAuthId = new EntityPropertyMain<>("authId", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epDisplayName = new EntityPropertyMain<>("displayName", TypeSimplePrimitive.EDM_STRING, false, true);
    public final NavigationPropertyEntity npPartyThing = new NavigationPropertyEntity("Party", false);
    public final NavigationPropertyEntitySet npThingsParty = new NavigationPropertyEntitySet("Things", npPartyThing);
    public final NavigationPropertyEntity npPartyGroup = new NavigationPropertyEntity("Party", false);
    public final NavigationPropertyEntitySet npGroupsParty = new NavigationPropertyEntitySet("ObservationGroups", npPartyGroup);
    public final NavigationPropertyEntity npPartyDatastream = new NavigationPropertyEntity("Party", false);
    public final NavigationPropertyEntitySet npDatastreamsParty = new NavigationPropertyEntitySet("Datastreams", npPartyDatastream);
    public final NavigationPropertyEntity npPartyMultiDatastream = new NavigationPropertyEntity("Party", false);
    public final NavigationPropertyEntitySet npMultiDatastreamsParty = new NavigationPropertyEntitySet("MultiDatastreams", npPartyMultiDatastream);
    public final EntityType etParty = new EntityType("Party", "Parties");
    /**
     * Class Campaign
     */
    public final EntityPropertyMain<String> epClassification = new EntityPropertyMain<>("classification", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<String> epCampaignTermsOfUse = new EntityPropertyMain<>("termsOfUse", TypeSimplePrimitive.EDM_STRING, true, false);
    public final EntityPropertyMain<String> epCampaignPrivacyPolicy = new EntityPropertyMain<>("privacyPolicy", TypeSimplePrimitive.EDM_STRING, false, true);
    public final EntityPropertyMain<TimeInstant> epCampaignCreationTime = new EntityPropertyMain<>("creationTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, true, false);
    public final EntityPropertyMain<TimeInstant> epCampaignStartTime = new EntityPropertyMain<>("startTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, false, true);
    public final EntityPropertyMain<TimeInstant> epCampaignEndTime = new EntityPropertyMain<>("endTime", TypeSimplePrimitive.EDM_DATETIMEOFFSET, false, true);
    public final EntityPropertyMain<String> epUrl = new EntityPropertyMain<>("url", TypeSimplePrimitive.EDM_STRING, false, true);
    public final NavigationPropertyEntitySet npCampaignDatastreams = new NavigationPropertyEntitySet("Campaigns");
    public final NavigationPropertyEntitySet npDatastreamsCampaign = new NavigationPropertyEntitySet("Datastreams", npCampaignDatastreams);
    public final NavigationPropertyEntitySet npCampaignMultiDatastreams = new NavigationPropertyEntitySet("Campaigns");
    public final NavigationPropertyEntitySet npMultiDatastreamsCampaign = new NavigationPropertyEntitySet("MultiDatastreams", npCampaignMultiDatastreams);
    public final EntityType etCampaign = new EntityType("Campaign", "Campaigns");
    public final NavigationPropertyEntity npPartyCampaign = new NavigationPropertyEntity("Party", false);
    public final NavigationPropertyEntitySet npCampaignsParty = new NavigationPropertyEntitySet("Campaigns", npPartyCampaign);

    public final NavigationPropertyEntitySet npCampaignsGroup = new NavigationPropertyEntitySet("Campaigns");
    public final NavigationPropertyEntitySet npGroupsCampaign = new NavigationPropertyEntitySet("ObservationGroups", npCampaignsGroup);

    // Type IDs
    public EntityPropertyMain<?> epIdGroup;
    public EntityPropertyMain<?> epIdLicense;
    public EntityPropertyMain<?> epIdParty;
    public EntityPropertyMain<?> epIdCampaign;
    public EntityPropertyMain<?> epIdRelation;
    private CoreSettings settings;
    private PluginPlusSettings plusSettings;
    private PluginCoreModel pluginCoreModel;
    private PluginMultiDatastream pluginMultiDatastream;
    private boolean enabled;
    private boolean enforceOwnership;
    private boolean enforceLicensing;
    private boolean enforceGroupLicensing;
    private boolean fullyInitialised;
    private URL licenseDomain;

    private boolean authEnabled;

    public PluginPLUS() {
        LOGGER.info("Creating new STAplus Plugin.");
    }

    @Override
    public InitResult init(CoreSettings settings) {
        this.settings = settings;
        Settings pluginSettings = settings.getPluginSettings();
        enabled = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_PLUS, PluginPlusSettings.class);
        if (!enabled) {
            return InitResult.INIT_OK;
        }
        enforceOwnership = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_ENFORCE_OWNERSHIP, PluginPlusSettings.class);
        enforceLicensing = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_ENFORCE_LICENSING, PluginPlusSettings.class);
        enforceGroupLicensing = pluginSettings.getBoolean(PluginPlusSettings.TAG_ENABLE_ENFORCE_GROUP_LICENSING, PluginPlusSettings.class);

        if (enforceLicensing || enforceGroupLicensing) {
            LOGGER.info("Setting plugins.plus.idType.license, using value 'String'.");
            pluginSettings.set(PluginPlusSettings.TAG_ID_TYPE_LICENSE, "String");
            try {
                licenseDomain = new URL(pluginSettings.get(PluginPlusSettings.TAG_ENABLE_LICENSE_DOMAIN, PluginPlusSettings.class));
            } catch (MalformedURLException e) {
                LOGGER.error("value for '" + PluginPlusSettings.TAG_ENABLE_LICENSE_DOMAIN + "' not a valid URL");
            }
        }

        Settings authSettings = settings.getAuthSettings();
        authEnabled = (!authSettings.get("provider", "").equalsIgnoreCase(""));

        plusSettings = new PluginPlusSettings(settings);
        settings.getPluginManager().registerPlugin(this);

        pluginCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        pluginMultiDatastream = settings.getPluginManager().getPlugin(PluginMultiDatastream.class);

        final ModelRegistry mr = settings.getModelRegistry();
        mr.registerPropertyType(propertyTypeRole);
        /**
         * Class License
         */
        epIdLicense = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(plusSettings.idTypeLicense)).setAliases("id");
        npDatastreamsLicense.setEntityType(pluginCoreModel.etDatastream);
        npGroupsLicense.setEntityType(etGroup);
        npCampaignsLicense.setEntityType(etCampaign);
        etLicense
                .registerProperty(epIdLicense)
                .registerProperty(pluginCoreModel.epName)
                .registerProperty(epLicenseDescription)
                .registerProperty(epLicenseDefinition)
                .registerProperty(epLicenseLogo)
                .registerProperty(epLicenseAttributionText)
                .registerProperty(npDatastreamsLicense)
                .registerProperty(npGroupsLicense)
                .registerProperty(npCampaignsLicense)
                .addCreateValidator(etLicense.entityName + ".createValidator", (entity) -> {

                    if (!enforceLicensing)
                        return;

                    if ((entity.getPrimaryKeyValues().get(0) != null) && LICENSE_IDS.contains(entity.getPrimaryKeyValues().get(0).toString()))
                        throw new ForbiddenException("License with this `id` cannot be created.");
                })
                .addUpdateValidator(etParty.entityName + ".updateValidator", (entity) -> {

                    if (!enforceLicensing)
                        return;

                    if ((entity.getPrimaryKeyValues().get(0) != null) && LICENSE_IDS.contains(entity.getPrimaryKeyValues().get(0).toString()))
                        throw new ForbiddenException("License with this `id` cannot be updated.");
                });

        npLicenseDatastream.setEntityType(etLicense);
        pluginCoreModel.etDatastream.registerProperty(npLicenseDatastream);

        /**
         * Class Party
         */
        epIdParty = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(plusSettings.idTypeParty)).setAliases("id");
        npThingsParty.setEntityType(pluginCoreModel.etThing);
        npGroupsParty.setEntityType(etGroup);
        npCampaignsParty.setEntityType(etCampaign);
        npDatastreamsParty.setEntityType(pluginCoreModel.etDatastream);
        etParty
                .registerProperty(epIdParty)
                .registerProperty(epPartyDescription)
                .registerProperty(epAuthId)
                .registerProperty(epDisplayName)
                .registerProperty(epPartyRole)
                .registerProperty(npThingsParty)
                .registerProperty(npGroupsParty)
                .registerProperty(npCampaignsParty)
                .registerProperty(npDatastreamsParty)
                .addCreateValidator(etParty.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (principal == null)
                        throw new UnauthorizedException("Authentication required. Please configure 'auth.provider'");

                    if (isAdmin(principal))
                        return;

                    String userId = principal.getName();

                    // Make sure userId is in UUID format
                    try {
                        // This throws exception if userId is not in UUID format
                        UUID.fromString(userId);
                    } catch (IllegalArgumentException exception) {
                        userId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
                    }

                    if (entity.isSetProperty(epAuthId)) {
                        String authId = entity.getProperty(epAuthId);
                        // Make sure Party.authId is in UUID format
                        try {
                            // This throws exception if authId is not in UUID format
                            UUID.fromString(authId);
                        } catch (IllegalArgumentException exception) {
                            entity.setProperty(epAuthId, UUID.nameUUIDFromBytes(authId.getBytes()).toString());
                        }
                    }

                    if ((entity.isSetProperty(epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty(epAuthId)))) {
                        // The authId is set by this plugin - it cannot be different from the POSTed Party property authId
                        throw new IllegalArgumentException("Party property 'authId' must represent the acting user or be omitted");
                    }

                })
                .addUpdateValidator(etParty.entityName + ".updateValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal)) {
                        // An admin can override the authId of any Party
                        String authId = entity.getProperty(epAuthId);
                        if (authId == null)
                            return;

                        // This throws exception if POSTed authId is not in UUID format
                        UUID.fromString(authId);
                        return;
                    }

                    String userId = principal.getName();
                    if ((entity.isSetProperty(epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty(epAuthId)))) {
                        // The authId is set by the plugin - it cannot be changed via a PATCH
                        throw new ForbiddenException("Party property 'authId' cannot be changed");
                    }

                });

        /**
         * Class Thing
         */
        npPartyThing.setEntityType(etParty);
        pluginCoreModel.etThing
                .registerProperty(npPartyThing)
                .addCreateValidator(pluginCoreModel.etThing.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    Entity party = entity.getProperty(npPartyThing);
                    if (party != null)
                        assertOwnership(entity, party, principal);

                })
                .addUpdateValidator(pluginCoreModel.etThing.entityName + ".updateValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    Entity party = entity.getProperty(npPartyThing);
                    if (party != null)
                        assertOwnership(entity, party, principal);
                    else
                        assertPrincipal(principal);

                });

        /**
         * Class Datastream
         */
        npPartyDatastream.setEntityType(etParty);
        pluginCoreModel.etDatastream
                .registerProperty(npPartyDatastream)
                .addCreateValidator(pluginCoreModel.etDatastream.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    Entity party = entity.getProperty(npPartyDatastream);
                    if (party != null)
                        assertOwnership(entity, party, principal);

                })
                .addUpdateValidator(pluginCoreModel.etDatastream.entityName + "updateValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    Entity party = entity.getProperty(npPartyDatastream);
                    if (party != null)
                        assertOwnership(entity, party, principal);
                    else
                        assertPrincipal(principal);

                });

        /**
         * Class Campaign
         */
        epIdCampaign = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(plusSettings.idTypeCampaign)).setAliases("id");
        npLicenseCampaign.setEntityType(etLicense);
        npPartyCampaign.setEntityType(etParty);
        npDatastreamsCampaign.setEntityType(pluginCoreModel.etDatastream);
        npGroupsCampaign.setEntityType(etGroup);
        etCampaign
                .registerProperty(epIdCampaign)
                .registerProperty(pluginCoreModel.epName)
                .registerProperty(pluginCoreModel.epDescription)
                .registerProperty(ModelRegistry.EP_PROPERTIES)
                .registerProperty(epClassification)
                .registerProperty(epCampaignTermsOfUse)
                .registerProperty(epCampaignPrivacyPolicy)
                .registerProperty(epCampaignCreationTime)
                .registerProperty(epCampaignStartTime)
                .registerProperty(epCampaignEndTime)
                .registerProperty(epUrl)
                .registerProperty(npLicenseCampaign)
                .registerProperty(npPartyCampaign)
                .registerProperty(npDatastreamsCampaign)
                .registerProperty(npGroupsCampaign)
                .addCreateValidator(etCampaign.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    assertOwnership(entity, entity.getProperty(npPartyCampaign), principal);

                })
                .addUpdateValidator(etCampaign.entityName + ".updateValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    assertOwnership(entity, entity.getProperty(npPartyCampaign), principal);

                });

        npCampaignDatastreams.setEntityType(etCampaign);
        pluginCoreModel.etDatastream.registerProperty(npCampaignDatastreams);

        if (pluginMultiDatastream != null) {
            etCampaign.registerProperty(npMultiDatastreamsCampaign);
            npCampaignMultiDatastreams.setEntityType(etCampaign);
            npMultiDatastreamsCampaign.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream.registerProperty(npCampaignMultiDatastreams);
        }

        /**
         * Class Group
         */
        epIdGroup = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(plusSettings.idTypeGroup)).setAliases("id");
        npPartyGroup.setEntityType(etParty);
        npLicenseGroup.setEntityType(etLicense);
        npCampaignsGroup.setEntityType(etCampaign);
        etGroup
                .registerProperty(epIdGroup)
                .registerProperty(pluginCoreModel.epName)
                .registerProperty(pluginCoreModel.epDescription)
                .registerProperty(ModelRegistry.EP_PROPERTIES)
                .registerProperty(epGroupPurpose)
                .registerProperty(epGroupCreationTime)
                .registerProperty(epGroupEndTime)
                .registerProperty(epGroupTermsOfUse)
                .registerProperty(epGroupPrivacyPolicy)
                .registerProperty(epGroupDataQuality)
                .registerProperty(npObservationsGroup)
                .registerProperty(npRelationsGroup)
                .registerProperty(npLicenseGroup)
                .registerProperty(npPartyGroup)
                .registerProperty(npCampaignsGroup)
                .addCreateValidator(etGroup.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    assertOwnership(entity, entity.getProperty(npPartyGroup), principal);

                })
                .addUpdateValidator(etGroup.entityName + ".updateValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    assertOwnership(entity, entity.getProperty(npPartyGroup), principal);

                });

        npObservationGroups.setEntityType(etGroup);
        pluginCoreModel.etObservation.registerProperty(npObservationGroups);

        /**
         * Class Relation
         */
        epIdRelation = new EntityPropertyMain<>(AT_IOT_ID, mr.getPropertyType(plusSettings.idTypeRelation)).setAliases("id");
        npSubjectRelation.setEntityType(pluginCoreModel.etObservation);
        npObjectRelation.setEntityType(pluginCoreModel.etObservation);
        npRelationGroups.setEntityType(etGroup);
        etRelation
                .registerProperty(epIdRelation)
                .registerProperty(pluginCoreModel.epDescription)
                .registerProperty(ModelRegistry.EP_PROPERTIES)
                .registerProperty(epRelationRole)
                .registerProperty(epExternalObject)
                .registerProperty(npSubjectRelation)
                .registerProperty(npObjectRelation)
                .registerProperty(npRelationGroups);

        npObjectsObservation.setEntityType(etRelation);
        npSubjectsObservation.setEntityType(etRelation);
        npRelationsGroup.setEntityType(etRelation);

        // link subject and object to Relations
        pluginCoreModel.etObservation
                .registerProperty(npObjectsObservation)
                .registerProperty(npSubjectsObservation)
                .addUpdateValidator(pluginCoreModel.etObservation.entityName + ".createValidator", (entity) -> {

                    if (!enforceOwnership)
                        return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    Entity datastream = entity.getProperty(pluginCoreModel.npDatastreamObservation);
                    if (datastream != null)
                        assertOwnership(entity, datastream.getProperty(npPartyDatastream), principal);

                    if (pluginMultiDatastream != null) {
                        Entity multiDatastream = entity.getProperty(pluginMultiDatastream.npMultiDatastreamObservation);
                        if (multiDatastream != null)
                            assertOwnership(entity, multiDatastream.getProperty(npPartyDatastream), principal);
                    }

                });

        if ((pluginMultiDatastream != null)) {
            /**
             * Class License
             */
            npLicenseMultiDatastream.setEntityType(etLicense);
            npMultiDatastreamsLicense.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream.registerProperty(npLicenseMultiDatastream);

            etLicense.registerProperty(npMultiDatastreamsLicense);

            /**
             * Class Party
             */
            npPartyMultiDatastream.setEntityType(etParty);
            npMultiDatastreamsParty.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream
                    .registerProperty(npPartyMultiDatastream)
                    .addCreateValidator(pluginMultiDatastream.etMultiDatastream.entityName + ".createValidator", (entity) -> {

                        if (!enforceOwnership)
                            return;

                        Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                        if (isAdmin(principal))
                            return;

                        assertOwnership(entity, entity.getProperty(npPartyMultiDatastream), principal);

                    })
                    .addUpdateValidator(pluginMultiDatastream.etMultiDatastream.entityName + ".createValidator", (entity) -> {

                        if (!enforceOwnership)
                            return;

                        Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                        if (isAdmin(principal))
                            return;

                        assertOwnership(entity, entity.getProperty(npPartyMultiDatastream), principal);

                    });

            etParty.registerProperty(npMultiDatastreamsParty);

            /**
             * Class Campaign
             */
            npCampaignMultiDatastreams.setEntityType(etCampaign);
            npMultiDatastreamsCampaign.setEntityType(pluginMultiDatastream.etMultiDatastream);
            pluginMultiDatastream.etMultiDatastream.registerProperty(npCampaignMultiDatastreams);

            etCampaign.registerProperty(npMultiDatastreamsCampaign);
        }
        return InitResult.INIT_OK;
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

        if (this.enforceOwnership) {
            extensionList.add("http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/business-logic");
            serverSettings.put("http://www.opengis.net/spec/sensorthings-staplus/1.0/conf/business-logic", new HashMap<>(Map.of("href", REQUIREMENT_ENFORCE_OWNERSHIP)));
        }

        if (this.enforceLicensing)
            extensionList.add(REQUIREMENT_ENFORCE_LICENSING);

        if (this.enforceGroupLicensing)
            extensionList.add(REQUIREMENT_ENFORCE_GROUP_LICENSING);

        if (this.authEnabled)
            extensionList.add(REQUIREMENT_AUTH);
    }

    @Override
    public void registerEntityTypes() {
        LOGGER.info("Initialising PLUS Types...");
        ModelRegistry modelRegistry = settings.getModelRegistry();
        modelRegistry.registerEntityType(etLicense);
        modelRegistry.registerEntityType(etGroup);
        modelRegistry.registerEntityType(etRelation);
        modelRegistry.registerEntityType(etParty);
        modelRegistry.registerEntityType(etCampaign);
    }

    @Override
    public boolean linkEntityTypes(PersistenceManager pm) {
        LOGGER.info("Linking PLUS Types...");

        if (pluginCoreModel == null || !pluginCoreModel.isFullyInitialised()) {
            return false;
        }

        if (pm instanceof JooqPersistenceManager ppm) {
            TableCollection tableCollection = ppm.getTableCollection();
            final DataType dataTypeLicense = ppm.getDataTypeFor(plusSettings.idTypeLicense);
            final DataType dataTypeGroup = ppm.getDataTypeFor(plusSettings.idTypeGroup);
            final DataType dataTypeRelation = ppm.getDataTypeFor(plusSettings.idTypeRelation);
            final DataType dataTypeParty = ppm.getDataTypeFor(plusSettings.idTypeParty);
            final DataType dataTypeCampaign = ppm.getDataTypeFor(plusSettings.idTypeCampaign);
            final DataType dataTypeObservation = tableCollection.getTableForType(pluginCoreModel.etObservation).getPkFields().get(0).getDataType();
            final DataType dataTypeDatastream = tableCollection.getTableForType(pluginCoreModel.etDatastream).getPkFields().get(0).getDataType();
            /**
             * Class License
             */
            tableCollection.registerTable(etLicense, new TableImpLicense(dataTypeLicense, this, pluginCoreModel));

            /**
             * Class Group
             */
            tableCollection.registerTable(etGroup, new TableImpGroup(dataTypeGroup, dataTypeParty, dataTypeLicense, this, pluginCoreModel, pluginMultiDatastream));
            tableCollection.registerTable(new TableImpGroupsObservations(dataTypeGroup, dataTypeObservation));
            tableCollection.registerTable(new TableImpGroupsRelations(dataTypeGroup, dataTypeRelation));
            tableCollection.registerTable(new TableImpGroupsCampaigns(dataTypeGroup, dataTypeCampaign));

            /**
             * Class Relation
             */
            tableCollection.registerTable(etRelation, new TableImpRelation(dataTypeRelation, dataTypeObservation, dataTypeGroup, this, pluginCoreModel));

            /**
             * Class Party
             */
            tableCollection.registerTable(etParty, new TableImpParty(dataTypeParty, this, pluginCoreModel, pluginMultiDatastream));

            /**
             * Class Campaign
             */
            tableCollection.registerTable(etCampaign, new TableImpCampaign(dataTypeCampaign, dataTypeParty, dataTypeLicense, this, pluginCoreModel, pluginMultiDatastream));
            tableCollection.registerTable(new TableImpCampaignsDatastreams(dataTypeCampaign, dataTypeDatastream));
            if (pluginMultiDatastream != null) {
                final DataType dataTypeMultiDatastream = tableCollection.getTableForType(pluginMultiDatastream.etMultiDatastream).getPkFields().get(0).getDataType();
                tableCollection.registerTable(new TableImpCampaignsMultiDatastreams(dataTypeCampaign, dataTypeMultiDatastream));
            }

            /*
             * Table Helpers
             */

            new TableHelperParty(settings, ppm).registerPreHooks();
            new TableHelperDatastream(settings, ppm).registerPreHooks();
            new TableHelperMultiDatastream(settings, ppm).registerPreHooks();
            new TableHelperThing(settings, ppm).registerPreHooks();
            new TableHelperGroup(settings, ppm).registerPreHooks();
            new TableHelperObservation(settings, ppm).registerPreHooks();
            new TableHelperLocation(settings, ppm).registerPreHooks();
            new TableHelperFeatureOfInterest(settings, ppm).registerPreHooks();
            new TableHelperLicense(settings, ppm).registerPreHooks();
            new TableHelperCampaign(settings, ppm).registerPreHooks();
            new TableHelperRelation(settings, ppm).registerPreHooks();
        }
        fullyInitialised = true;
        return true;
    }

    public Map<String, Object> createLiqibaseParams(JooqPersistenceManager ppm, Map<String, Object> target) {
        if (target == null) {
            target = new LinkedHashMap<>();
        }
        pluginCoreModel.createLiqibaseParams(ppm, target);
        if (pluginMultiDatastream == null) {
            // Create placeholder variables, otherwise Liquibase complains.
            ppm.generateLiquibaseVariables(target, "MultiDatastream", plusSettings.idTypeDefault);
        } else {
            pluginMultiDatastream.createLiqibaseParams(ppm, target);
        }
        ppm.generateLiquibaseVariables(target, "Group", plusSettings.idTypeGroup);
        ppm.generateLiquibaseVariables(target, "License", plusSettings.idTypeLicense);
        ppm.generateLiquibaseVariables(target, "Party", plusSettings.idTypeParty);
        ppm.generateLiquibaseVariables(target, "Campaign", plusSettings.idTypeCampaign);
        ppm.generateLiquibaseVariables(target, "Relation", plusSettings.idTypeRelation);

        return target;
    }

    @Override
    public String checkForUpgrades() {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof JooqPersistenceManager ppm) {
            return ppm.checkForUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null));
        }
        return "Unknown persistence manager class";
    }

    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
        PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create();
        if (pm instanceof JooqPersistenceManager ppm) {
            return ppm.doUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null), out);
        }
        out.append("Unknown persistence manager class");
        return false;
    }

    public boolean isEnforceOwnershipEnabled() {
        return enforceOwnership;
    }

    public boolean isEnforceLicensingEnabled() {
        return enforceLicensing;
    }

    public boolean isEnforceGroupLicensingEnabled() {
        return enforceGroupLicensing;
    }

    public URL getLicenseDomain() {
        return licenseDomain;
    }

    private boolean isAdmin(Principal principal) {
        if (principal == null)
            return false;

        return ((principal instanceof PrincipalExtended) && ((PrincipalExtended) principal).isAdmin());

    }

    private void assertPrincipal(Principal principal) {
        if (principal == null)
            throw new UnauthorizedException("No Principal");
    }

    private void assertAdmin(Principal principal) {
        assertPrincipal(principal);

        if (!isAdmin(principal))
            throw new UnauthorizedException("User is not admin");
    }

    private void assertOwnership(Entity entity, Entity party, Principal principal) {
        assertPrincipal(principal);
        String principalId = principal.getName();

        if (party != null) {
            PkValue partyId = party.getPrimaryKeyValues();
            String authId = party.getProperty(epAuthId);

            if ((partyId == null) && (authId == null) && (principalId == null))
                throw new IllegalArgumentException("Party not identified");

            if ((partyId == null) && (authId == null) && (principalId != null)) {
                party.setPrimaryKeyValues(PkValue.of(principalId));
                return;
            }

            if ((authId != null) && (partyId != null)) {
                LOGGER.warn("Party identified by @iot.id and authId - using authId");
            }

            if (authId != null)
                party.setPrimaryKeyValues(PkValue.of(authId));

            String userId = principal.getName();
            if (!userId.equalsIgnoreCase(party.getPrimaryKeyValues().get(0).toString())) {
                // The Id of the Party must match the userId
                // Entity can only be associated to the Party identifying the acting user
                throw new ForbiddenException(entity.getEntityType().entityName + " can only be associated with the Party identifying the acting user");
            }

        }

    }

    /**
     * Class Party
     */
    public enum Role {
        individual,
        institutional
    }
}
