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
import de.fraunhofer.iosb.ilt.frostserver.model.core.IdString;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
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
import org.jooq.Field;

public class TableHelperParty extends TableHelper {

    private final TableImpParty tableParties;

    public TableHelperParty(CoreSettings settings, PostgresPersistenceManager ppm) {
        super(settings, ppm);

        this.tableParties = tables.getTableForClass(TableImpParty.class);
    }

    @Override
    public void registerPreHooks() {

        tableParties.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(PostgresPersistenceManager pm, Entity entity,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled()) {
                    // test if the authId is set
                    if (!entity.isSetProperty(pluginPlus.epAuthId))
                        return true;

                    // make sure that the Party's iot.id is equal to the authId
                    String authID = entity.getProperty(pluginPlus.epAuthId);
                    entity.setId(new IdString(authID));
                    // If the Party already exist, we can skip processing
                    return pm.get(pluginPlus.etParty, entity.getId()) == null;
                }

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal)) {
                    // The admin has extra rights
                    String authID = entity.getProperty(pluginPlus.epAuthId);
                    entity.setId(new IdString(authID));
                    Entity party = pm.get(pluginPlus.etParty, entity.getId());
                    // No need to insert the entity as it already exists. Just return the Id of the existing Party
                    return party == null;
                }

                // We have a username available from the Principal
                assertPrincipal(principal);
                String userId = principal.getName();

                if ((entity.isSetProperty(pluginPlus.epAuthId)) && (!userId.equalsIgnoreCase(entity.getProperty((pluginPlus.epAuthId))))) {
                    // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                    throw new IllegalArgumentException("Party property authId cannot be set");
                }

                entity.setProperty(pluginPlus.epAuthId, userId);
                entity.setId(new IdString(userId));
                Entity party = pm.get(pluginPlus.etParty, entity.getId());
                // No need to insert the entity as it already exists. Just return the Id of the existing Party
                return party == null;

            }
        });

        tableParties.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Object entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                // We have a username available from the Principal
                assertPrincipal(principal);
                String userId = principal.getName();

                if (!userId.equalsIgnoreCase(entity.getId().toString())) {
                    // The authId is set by this plugin - it cannot be set via POSTed Party property authId
                    throw new ForbiddenException("Cannot update existing Party of another user");
                }

                entity.setProperty(pluginPlus.epAuthId, userId);
                entity.setId(new IdString(userId));
            }
        });

        tableParties.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(PostgresPersistenceManager pm, Object entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled())
                    return;

                Principal principal = ServiceRequest.LOCAL_REQUEST.get().getUserPrincipal();

                if (isAdmin(principal))
                    return;

                throw new ForbiddenException("Deleting Party is not allowed");
            }
        });

    }

}
