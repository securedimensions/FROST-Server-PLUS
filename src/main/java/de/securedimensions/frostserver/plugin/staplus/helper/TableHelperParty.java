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
package de.securedimensions.frostserver.plugin.staplus.helper;

import static de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert.Phase.PRE_RELATIONS;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.PkValue;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.securedimensions.frostserver.plugin.staplus.TableImpParty;
import java.security.Principal;
import java.util.UUID;

public class TableHelperParty extends TableHelper {

    private final TableImpParty tableParties;

    public TableHelperParty(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableParties = tables.getTableForClass(TableImpParty.class);
    }

    @Override
    public void registerPreHooks() {

        tableParties.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS)
                        return true;

                    if (entity.isSetProperty(pluginPlus.epAuthId)) {
                        String authID = entity.getProperty(pluginPlus.epAuthId);
                        // Make sure that the authI is in UUID format
                        try {
                            // This throws exception if userId is not in UUID format
                            UUID.fromString(authID);
                        } catch (IllegalArgumentException exception) {
                            // In case the userId is not a UUID
                            entity.setProperty(pluginPlus.epAuthId, UUID.nameUUIDFromBytes(authID.getBytes()).toString());
                        }
                    }

                    if (!pluginPlus.isEnforceOwnershipEnabled()) {
                        // test if the authId is set
                        if (!entity.isSetProperty(pluginPlus.epAuthId))
                            return true;

                        // make sure that the Party's iot.id is equal to the authId
                        String authID = entity.getProperty(pluginPlus.epAuthId);

                        entity.setPrimaryKeyValues(PkValue.of(authID));
                        // If the Party already exist, we can skip processing
                        return pm.get(pluginPlus.etParty, entity.getPrimaryKeyValues()) == null;
                    }

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal)) {
                        // The admin has extra rights
                        String authID = entity.getProperty(pluginPlus.epAuthId);
                        entity.setPrimaryKeyValues(PkValue.of(authID));
                        entity = pm.get(pluginPlus.etParty, entity.getPrimaryKeyValues());
                        // No need to insert the entity as it already exists. Just return the Id of the existing Party
                        return (entity == null);
                    }

                    // We have a username available from the Principal
                    assertPrincipal(principal);
                    String userId = principal.getName();

                    // Make sure that the userId is in UUID format
                    try {
                        // This throws exception if userId is not in UUID format
                        UUID.fromString(userId);
                    } catch (IllegalArgumentException exception) {
                        // In case the userId is not a UUID
                        userId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
                    }

                    if ((entity.isSetProperty(pluginPlus.epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty((pluginPlus.epAuthId))))) {
                        // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                        throw new IllegalArgumentException("Party property 'authId' must represent the acting user or be omitted");
                    }

                    if (entity.isSetProperty(pluginPlus.npDatastreamsParty)) {
                        EntitySet ds = entity.getProperty(pluginPlus.npDatastreamsParty);
                        if (ds == null) {
                            throw new IllegalArgumentException("Datastreams do not exist.");
                        }
                        for (Entity d : ds) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertOwnershipDatastream(pm, d, principal);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertLicenseDatastream(pm, d);
                                assertEmptyDatastream(pm, d);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npMultiDatastreamsParty)) {
                        EntitySet mds = entity.getProperty(pluginPlus.npMultiDatastreamsParty);
                        if (mds == null) {
                            throw new IllegalArgumentException("MultiDatastreams do not exist.");
                        }
                        for (Entity md : mds) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertOwnershipMultiDatastream(pm, md, principal);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertLicenseMultiDatastream(pm, md);
                                assertEmptyMultiDatastream(pm, md);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npCampaignsParty)) {
                        EntitySet ps = entity.getProperty(pluginPlus.npCampaignsParty);
                        if (ps == null) {
                            throw new IllegalArgumentException("Campaigns do not exist.");
                        }
                        for (Entity p : ps) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertLicenseCampaign(pm, p);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertOwnershipCampaign(pm, p, principal);
                                assertEmptyCampaign(pm, p);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npGroupsParty)) {
                        EntitySet gs = entity.getProperty(pluginPlus.npGroupsParty);
                        if (gs == null) {
                            throw new IllegalArgumentException("Groups do not exist.");
                        }
                        for (Entity g : gs) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertLicenseGroup(pm, g);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertOwnershipGroup(pm, g, principal);
                                assertEmptyGroup(pm, g);
                            }
                        }
                    }
                    //else
                    //  throw new ForbiddenException("License must be associated with `Datastream`, `MultiDatastream`, `Campaign` or `Group`.");

                    entity.setProperty(pluginPlus.epAuthId, userId);
                    entity.setPrimaryKeyValues(PkValue.of(userId));
                    entity = pm.get(pluginPlus.etParty, entity.getPrimaryKeyValues());
                    // No need to insert the entity if it already exists:
                    return (entity == null);

                });

        tableParties.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

                    //if (!pluginPlus.isEnforceOwnershipEnabled())
                    //return;

                    Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                    if (isAdmin(principal))
                        return;

                    // We have a username available from the Principal
                    assertPrincipal(principal);
                    String userId = principal.getName();

                    if (!userId.equalsIgnoreCase(entityId.get(0).toString())) {
                        // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                        throw new ForbiddenException("Cannot update existing Party of another user");
                    }

                    if (entity.isSetProperty(pluginPlus.npDatastreamsParty)) {
                        EntitySet ds = entity.getProperty(pluginPlus.npDatastreamsParty);
                        if (ds == null) {
                            throw new IllegalArgumentException("Datastreams do not exist.");
                        }
                        for (Entity d : ds) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertOwnershipDatastream(pm, d, principal);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertLicenseDatastream(pm, d);
                                assertEmptyDatastream(pm, d);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npMultiDatastreamsParty)) {
                        EntitySet mds = entity.getProperty(pluginPlus.npMultiDatastreamsParty);
                        if (mds == null) {
                            throw new IllegalArgumentException("MultiDatastreams do not exist.");
                        }
                        for (Entity md : mds) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertOwnershipMultiDatastream(pm, md, principal);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertLicenseMultiDatastream(pm, md);
                                assertEmptyMultiDatastream(pm, md);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npCampaignsParty)) {
                        EntitySet ps = entity.getProperty(pluginPlus.npCampaignsParty);
                        if (ps == null) {
                            throw new IllegalArgumentException("Campaigns do not exist.");
                        }
                        for (Entity p : ps) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertLicenseCampaign(pm, p);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertOwnershipCampaign(pm, p, principal);
                                assertEmptyCampaign(pm, p);
                            }
                        }
                    } else if (entity.isSetProperty(pluginPlus.npGroupsParty)) {
                        EntitySet gs = entity.getProperty(pluginPlus.npGroupsParty);
                        if (gs == null) {
                            throw new IllegalArgumentException("Groups do not exist.");
                        }
                        for (Entity g : gs) {
                            if (pluginPlus.isEnforceOwnershipEnabled()) {
                                assertLicenseGroup(pm, g);
                            }
                            if (pluginPlus.isEnforceLicensingEnabled()) {
                                assertOwnershipGroup(pm, g, principal);
                                assertEmptyGroup(pm, g);
                            }
                        }
                    }
                    //else
                    //throw new ForbiddenException("License must be associated with `Datastream`, `MultiDatastream`, `Campaign` or `Group`.");

                    entity.setProperty(pluginPlus.epAuthId, userId);
                    entity.setPrimaryKeyValues(PkValue.of(userId));

                });

        tableParties.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceOwnershipEnabled())
                return;

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

            if (isAdmin(principal))
                return;

            throw new ForbiddenException("Deleting Party is not allowed");
        });

    }

}
