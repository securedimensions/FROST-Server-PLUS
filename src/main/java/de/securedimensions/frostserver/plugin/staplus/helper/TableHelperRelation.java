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
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.JooqPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.ForbiddenException;
import de.securedimensions.frostserver.plugin.staplus.TableImpRelation;
import java.security.Principal;

public class TableHelperRelation extends TableHelper {

    private final TableImpRelation tableRelations;

    public TableHelperRelation(CoreSettings settings, JooqPersistenceManager ppm) {
        super(settings, ppm);

        this.tableRelations = tables.getTableForClass(TableImpRelation.class);
    }

    @Override
    public void registerPreHooks() {

        tableRelations.registerHookPreInsert(-1,
                (phase, pm, entity, insertFields) -> {

                    /*
                     * Select Phase
                     */
                    if (phase == PRE_RELATIONS)
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

                });

        tableRelations.registerHookPreUpdate(-1,
                (pm, entity, entityId, updateMode) -> {

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

                });

        tableRelations.registerHookPreDelete(-1, (pm, entityId) -> {

            if (!pluginPlus.isEnforceOwnershipEnabled()) {
                return;
            }

            Principal principal = ServiceRequest.getLocalRequest().getUserPrincipal();
            if (isAdmin(principal)) {
                return;
            }

            Entity subject = pm.get(pluginPlus.etRelation, entityId).getProperty(pluginPlus.npSubjectRelation);
            Entity ds = pm.get(pluginCoreModel.etObservation, subject.getPrimaryKeyValues()).getProperty(pluginCoreModel.npDatastreamObservation);
            Entity party = pm.get(pluginCoreModel.etDatastream, ds.getPrimaryKeyValues()).getProperty(pluginPlus.npPartyDatastream);

            if (party == null) {
                throw new IllegalArgumentException("The Subject associated to the Relation must have a Datastream associated to a Party.");
            }

            if (!principal.getName().equalsIgnoreCase((String) party.getPrimaryKeyValues().get(0).toString())) {
                throw new ForbiddenException("A Relation can only be created to Subject associated to the acting Party.");
            }

        });

    }

}
