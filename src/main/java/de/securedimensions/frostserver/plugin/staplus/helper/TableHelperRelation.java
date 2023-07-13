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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreDelete;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreInsert;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.factories.HookPreUpdate;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.NoSuchEntityException;
import de.securedimensions.frostserver.plugin.staplus.TableImpRelations;
import java.security.Principal;
import java.util.Map;
import org.jooq.Field;

public class TableHelperRelation extends TableHelper {

    private final TableImpRelations tableRelations;

    public TableHelperRelation(CoreSettings settings, PostgresPersistenceManager ppm) {
        super(settings, ppm);

        this.tableRelations = tables.getTableForClass(TableImpRelations.class);
    }

    @Override
    public void registerPreHooks() {

        tableRelations.registerHookPreInsert(-10.0, new HookPreInsert() {

            @Override
            public boolean insertIntoDatabase(Phase phase, PostgresPersistenceManager pm, Entity entity,
                    Map<Field, Object> insertFields) throws NoSuchEntityException, IncompleteEntityException {

                /*
                 * Select Phase
                 */
                if (phase == Phase.PRE_RELATIONS)
                    return true;

                Entity subject = entity.getProperty(pluginPlus.npSubjectRelation);
                if (subject == null) {
                    throw new IllegalArgumentException("A Relation must have a Subject.");
                }

                Entity object = entity.getProperty(pluginPlus.npObjectRelation);
                String extObject = entity.getProperty(pluginPlus.epExternalObject);
                if ((object == null) && (extObject == null)) {
                    throw new IllegalArgumentException("A Relation must either have an Object or externalObject.");
                }

                if ((object != null) && (extObject != null)) {
                    throw new IllegalArgumentException("A Relation must not have an Object and externalObject.");
                }

                if (!pluginPlus.isEnforceOwnershipEnabled()) {
                    return true;
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();
                if (isAdmin(principal)) {
                    return true;
                }

                /*
                 * Ownership concept: Subject must point to a Datastream owned by the acting user
                 */
                assertOwnershipObservation(pm, subject, principal);

                /*
                 * Ownership concept: All Group entities must be owned by the acting user
                 */
                EntitySet groups = entity.getProperty(pluginPlus.npRelationGroups);
                if (groups == null) {
                    return true;
                }

                for (Entity group : groups) {
                    /*
                     * we need to check for each group
                     */
                    assertOwnershipGroup(pm, group, principal);
                }

                return true;
            }
        });

        tableRelations.registerHookPreUpdate(-10.0, new HookPreUpdate() {

            @Override
            public void updateInDatabase(PostgresPersistenceManager pm, Entity entity, Id entityId)
                    throws NoSuchEntityException, IncompleteEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled()) {
                    return;
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();
                if (isAdmin(principal)) {
                    return;
                }

                /*
                 * Ownership concept: Subject must point to a Datastream owned by the acting user
                 */
                Entity subject = entity.getProperty(pluginPlus.npSubjectRelation);
                assertOwnershipObservation(pm, subject, principal);

            }
        });

        tableRelations.registerHookPreDelete(-10.0, new HookPreDelete() {

            @Override
            public void delete(PostgresPersistenceManager pm, Id entityId) throws NoSuchEntityException {

                if (!pluginPlus.isEnforceOwnershipEnabled()) {
                    return;
                }

                Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();
                if (isAdmin(principal)) {
                    return;
                }

                Entity subject = pm.get(pluginPlus.etRelation, entityId).getProperty(pluginPlus.npSubjectRelation);
                Entity ds = pm.get(pluginCoreModel.etObservation, subject.getId()).getProperty(pluginCoreModel.npDatastreamObservation);
                Entity party = pm.get(pluginCoreModel.etDatastream, ds.getId()).getProperty(pluginPlus.npPartyDatastream);

                if (party == null) {
                    throw new IllegalArgumentException("The Subject associated to the Relation must have a Datastream associated to a Party.");
                }

                if (!principal.getName().equalsIgnoreCase((String) party.getId().getValue())) {
                    throw new ForbiddenException("A Relation can only be created to Subject associated to the acting Party.");
                }

            }
        });

    }

}
