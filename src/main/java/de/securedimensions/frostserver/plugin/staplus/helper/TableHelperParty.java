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
package de.securedimensions.frostserver.plugin.staplus.helper;

import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdString;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.TableImpParty;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.jooq.Field;

public class TableHelperParty extends TableHelper {

    private final TableImpParty tableParties;

    public TableHelperParty(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableParties = tables.getTableForClass(TableImpParty.class);
    }

    @Override
    public void registerPreHooks() {

        tableParties.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, JooqPersistenceManager pm, Entity party,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                /*
                 * Select Phase
                 */
                if (phase == Phase.PRE_RELATIONS)
                    return true;

                if (party.isSetProperty(pluginPlus.epAuthId)) {
                    String authID = party.getProperty(pluginPlus.epAuthId);
                    // Make sure that the authI is in UUID format
                    try {
                        // This throws exception if userId is not in UUID format
                        UUID.fromString(authID);
                    } catch (IllegalArgumentException exception) {
                        // In case the userId is not a UUID
                        party.setProperty(pluginPlus.epAuthId, UUID.nameUUIDFromBytes(authID.getBytes()).toString());
                    }
                }

                if (!pluginPlus.isEnforceOwnershipEnabled()) {
                    // test if the authId is set
                    if (!party.isSetProperty(pluginPlus.epAuthId))
                        return true;

                    // make sure that the Party's iot.id is equal to the authId
                    String authID = party.getProperty(pluginPlus.epAuthId);
                    party.setId(new IdString(authID));
                    // If the Party already exist, we can skip processing
                    return pm.get(pluginPlus.etParty, party.getId()) == null;
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal)) {
                    // The admin has extra rights
                    String authID = party.getProperty(pluginPlus.epAuthId);
                    party.setId(new IdString(authID));
                    party = pm.get(pluginPlus.etParty, party.getId());
                    // No need to insert the entity as it already exists. Just return the Id of the existing Party
                    return (party == null);
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

                if ((party.isSetProperty(pluginPlus.epAuthId)) && (!userId.equalsIgnoreCase(party.getProperty((pluginPlus.epAuthId))))) {
                    // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                    throw new IllegalArgumentException("Party property 'authId' must represent the acting user or be omitted");
                }

                if (party.isSetProperty(pluginPlus.npDatastreamsParty)) {
                    EntitySet ds = party.getProperty(pluginPlus.npDatastreamsParty);
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
                } else if (party.isSetProperty(pluginPlus.npMultiDatastreamsParty)) {
                    EntitySet mds = party.getProperty(pluginPlus.npMultiDatastreamsParty);
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
                } else if (party.isSetProperty(pluginPlus.npCampaignsParty)) {
                    EntitySet ps = party.getProperty(pluginPlus.npCampaignsParty);
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
                } else if (party.isSetProperty(pluginPlus.npGroupsParty)) {
                    EntitySet gs = party.getProperty(pluginPlus.npGroupsParty);
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

                party.setProperty(pluginPlus.epAuthId, userId);
                party.setId(new IdString(userId));
                party = pm.get(pluginPlus.etParty, party.getId());
                // No need to insert the entity if it already exists:
                return (party == null);

            }
        });

        tableParties.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(JooqPersistenceManager pm, Entity party, Id partyId)
                    throws NoSuchEntityException, IncompleteEntityException {

                //if (!pluginPlus.isEnforceOwnershipEnabled())
                //return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // We have a username available from the Principal
                assertPrincipal(principal);
                String userId = principal.getName();

                if (!userId.equalsIgnoreCase(partyId.toString())) {
                    // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                    throw new ForbiddenException("Cannot update existing Party of another user");
                }

                if (party.isSetProperty(pluginPlus.npDatastreamsParty)) {
                    EntitySet ds = party.getProperty(pluginPlus.npDatastreamsParty);
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
                } else if (party.isSetProperty(pluginPlus.npMultiDatastreamsParty)) {
                    EntitySet mds = party.getProperty(pluginPlus.npMultiDatastreamsParty);
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
                } else if (party.isSetProperty(pluginPlus.npCampaignsParty)) {
                    EntitySet ps = party.getProperty(pluginPlus.npCampaignsParty);
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
                } else if (party.isSetProperty(pluginPlus.npGroupsParty)) {
                    EntitySet gs = party.getProperty(pluginPlus.npGroupsParty);
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

                party.setProperty(pluginPlus.epAuthId, userId);
                party.setId(new IdString(userId));
            }
        });

        tableParties.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(JooqPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                throw new ForbiddenException("Deleting Party is not allowed");
            }
        });

    }

}
